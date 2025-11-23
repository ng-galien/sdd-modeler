package io.statemodeler.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.cli.dto.MigrationDto;
import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.migration.ChatModelProvider;
import io.statemodeler.migration.LangChainMigrationGenerationService;
import io.statemodeler.migration.LangChainModelProvider;
import io.statemodeler.migration.MigrationOrchestrationService;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.nio.file.Path;
import io.statemodeler.cli.util.PathUtils;
// Locale not needed when using Jackson for JSON serialization
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * CLI command to generate migration scripts between two SDR versions.
 *
 * <p>
 * Usage: sdd-modeler migrate <from-hash|name:version> <to-hash|name:version>
 * [options]
 *
 * <p>
 * Examples:
 * <ul>
 * <li>sdd-modeler migrate orders:1.0.0 orders:2.0.0
 * <li>sdd-modeler migrate abc123 def456 --dialect postgres
 * <li>sdd-modeler migrate orders:1.0.0 orders:2.0.0 -o migration.sql
 * <li>sdd-modeler migrate abc123 def456 --llm ollama --model llama3.2
 * </ul>
 */
@Command(
        name = "migrate",
        description = "Generate migration script between two SDR versions",
        mixinStandardHelpOptions = true)
public class MigrateCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MigrateCommand.class);

    @Parameters(index = "0", description = "Source SDR hash or name:version")
    String fromIdentifier;

    @Parameters(index = "1", description = "Target SDR hash or name:version")
    String toIdentifier;

    @Option(
            names = {"--dialect", "-d"},
            description = "Database dialect (default: postgres)",
            defaultValue = "postgres")
    String dialect;

    @Option(
            names = {"--output", "-o"},
            description = "Output file path (default: stdout)")
    File outputFile;

    @Option(
            names = {"--output-json", "-j"},
            description = "Write migration JSON output to file (default: none)")
    File outputJson;

    @Option(
            names = {"--llm"},
            description = "LLM provider (ollama|openai)",
            defaultValue = "ollama")
    String llmProvider;

    @Option(
            names = {"--model"},
            description = "LLM model name (default: llama3.2)")
    String modelName;

    @Option(
            names = {"--force"},
            description = "Force regeneration even if migration already exists")
    boolean force;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        // Validate dialect
        if (!isValidDialect(dialect)) {
            spec.commandLine().getErr().println("ERROR: Unsupported dialect '" + dialect + "'");
            spec.commandLine().getErr().println("  Supported dialects: postgres");
            return 1;
        }

        // Resolve and normalize output paths relative to the current process
        Path resolvedOutput = outputFile == null ? null : PathUtils.resolveFromProcess(outputFile.toPath());
        Path resolvedOutputJson = outputJson == null ? null : PathUtils.resolveFromProcess(outputJson.toPath());
        if (resolvedOutput != null) outputFile = resolvedOutput.toFile();
        if (resolvedOutputJson != null) outputJson = resolvedOutputJson.toFile();

        return io.vavr.control.Try.withResources(() -> repositoryMixin.createRepository())
                .of(repository -> findSdr(repository, fromIdentifier)
                        .flatMap(fromSdrOpt -> fromSdrOpt
                                .map(Try::success)
                                .orElseGet(() -> Try.failure(
                                        new IllegalStateException("Source SDR not found: " + fromIdentifier))))
                        .flatMap(fromSdr -> findSdr(repository, toIdentifier)
                                .flatMap(toSdrOpt -> toSdrOpt.map(Try::success)
                                        .orElseGet(() -> Try.failure(
                                                new IllegalStateException("Target SDR not found: " + toIdentifier))))
                                .flatMap(toSdr -> {
                                    // Check if migration already exists
                                    if (!force) {
                                        var existingResult =
                                                repository.findMigration(fromSdr.schemaHash(), toSdr.schemaHash());
                                        if (existingResult.isSuccess()
                                                && existingResult.get().isPresent()) {
                                            logger.info("INFO: Migration already exists (use --force to regenerate)");
                                            var migration = existingResult.get().get();
                                            outputMigration(migration.migrationScript());
                                            outputJson(
                                                    migration.confidence(),
                                                    migration.comments(),
                                                    migration.migrationScript());
                                            return Try.success(0);
                                        }
                                    }
                                    return generateAndSaveMigration(repository, fromSdr, toSdr);
                                }))
                        .getOrElseThrow(e -> e))
                .fold(
                        throwable -> {
                            if (throwable instanceof NoClassDefFoundError) {
                                spec.commandLine().getErr().println("ERROR: LangChain4j dependencies not found");
                                spec.commandLine()
                                        .getErr()
                                        .println("  The 'migrate' command requires LangChain4j libraries.");
                                spec.commandLine()
                                        .getErr()
                                        .println("  Please ensure the following dependencies are available:");
                                spec.commandLine().getErr().println("    - dev.langchain4j:langchain4j:0.36.2");
                                spec.commandLine().getErr().println("    - dev.langchain4j:langchain4j-ollama:0.36.2");
                                spec.commandLine()
                                        .getErr()
                                        .println(
                                                "    - dev.langchain4j:langchain4j-openai:0.36.2 (if using --llm openai)");
                                spec.commandLine().getErr().println("  Missing class: " + throwable.getMessage());
                                return 1;
                            }
                            spec.commandLine().getErr().println("ERROR: Unexpected error");
                            spec.commandLine().getErr().println("  " + throwable.getMessage());
                            // throwable.printStackTrace(); // Keep stack trace for debugging if needed, or
                            // remove for cleaner CLI
                            return 1;
                        },
                        result -> result);
    }

    private Try<Integer> generateAndSaveMigration(
            io.statemodeler.repository.SdrRepository repository, SdrRecord fromSdr, SdrRecord toSdr) {
        return Try.of(() -> {
                    // Create LLM-based migration service
                    logger.info("INFO: Generating migration using {} LLM...", llmProvider);
                    ChatModel llmModel = createLlmModel();

                    var migrationGenerator = new LangChainMigrationGenerationService(llmModel);
                    var comparisonService = new DdlComparisonService();
                    var orchestrationService =
                            new MigrationOrchestrationService(migrationGenerator, comparisonService, repository);

                    return orchestrationService;
                })
                .flatMap(orchestrationService -> orchestrationService.generateAndSaveMigration(fromSdr, toSdr, dialect))
                .map(migration -> {
                    logger.info("SUCCESS: Migration generated and saved");
                    logger.info("  From: {}", fromSdr.schemaHash());
                    logger.info("  To:   {}", toSdr.schemaHash());
                    logger.info("  Dialect: {}", dialect);

                    outputMigration(migration.migrationScript());
                    outputJson(migration.confidence(), migration.comments(), migration.migrationScript());
                    return 0;
                });
    }

    private void outputJson(double confidence, String comments, String script) {
        if (outputJson != null) {
            try {
                // Ensure parent dirs exist
                PathUtils.ensureParentDirectoryExists(outputJson.toPath());
                ObjectMapper objectMapper = new ObjectMapper();
                var dto = new MigrationDto(confidence, comments, script);
                objectMapper.writeValue(outputJson, dto);
                logger.info("  JSON Output: {}", outputJson.getAbsolutePath());
            } catch (Exception e) {
                logger.warn("WARN: Failed to write JSON output: {}", e.getMessage());
            }
        }
    }

    /**
     * Find SDR by hash or name:version identifier.
     */
    private Try<Optional<SdrRecord>> findSdr(io.statemodeler.repository.SdrRepository repository, String identifier) {
        // Try as hash first
        if (!identifier.contains(":")) {
            return repository.findByHash(identifier);
        }

        // Parse name:version
        String[] parts = identifier.split(":", 2);
        String name = parts[0];
        String version = parts.length > 1 ? parts[1] : null;

        if (version != null) {
            return repository.findByNameAndVersion(name, version);
        } else {
            // Find latest version by name: get metadata list and fetch full record via hash
            return repository.findByName(name).flatMap(metadataList -> {
                if (metadataList == null || metadataList.isEmpty()) {
                    return Try.success(Optional.empty());
                }
                String hash = metadataList.get(0).schemaHash();
                return repository.findByHash(hash);
            });
        }
    }

    /**
     * Validate database dialect.
     */
    private boolean isValidDialect(String dialect) {
        return "postgres".equalsIgnoreCase(dialect);
    }

    /**
     * Create LLM model based on provider.
     */
    private ChatModel createLlmModel() {
        String effectiveModelName = modelName != null ? modelName : getDefaultModelName();
        // Default timeout
        final int timeoutSeconds = 300;

        // Currently support 'ollama' and 'openai'
        if (llmProvider == null || llmProvider.isBlank() || llmProvider.equalsIgnoreCase("ollama")) {
            ChatModelProvider provider = new LangChainModelProvider();
            return provider.createModel(effectiveModelName, 0.7, "http://localhost:11434", timeoutSeconds);
        }
        if (llmProvider.equalsIgnoreCase("openai")) {
            // Use OpenAI provider
            var apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("OPENAI_API_KEY is required for OpenAI provider");
            }
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(effectiveModelName)
                    .logRequests(true)
                    .logResponses(true)
                    .strictJsonSchema(true)
                    .build();
        }

        throw new IllegalArgumentException("Unsupported LLM provider: " + llmProvider);
    }

    /**
     * Get default model name for provider.
     */
    private String getDefaultModelName() {
        return "llama3.2";
    }

    /**
     * Output migration script to file or stdout.
     */
    private void outputMigration(String script) {
        if (outputFile != null) {
            try {
                PathUtils.ensureParentDirectoryExists(outputFile.toPath());
            } catch (Exception e) {
                logger.warn("WARN: Failed to create output directory: {}", e.getMessage());
            }
            var writeResult = io.vavr.control.Try.of(() -> Files.writeString(outputFile.toPath(), script));
            writeResult.onFailure(e -> logger.warn("WARN: Failed to write output: {}", e.getMessage()));
            if (writeResult.isFailure()) {
                spec.commandLine().getOut().println(script);
            } else {
                logger.info("  Output: {}", outputFile.getAbsolutePath());
            }
        } else {
            spec.commandLine().getOut().println(script);
        }
    }
}
