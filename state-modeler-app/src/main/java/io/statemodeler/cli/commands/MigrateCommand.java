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
// Locale not needed when using Jackson for JSON serialization
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command to generate migration scripts between two SDR versions.
 *
 * <p>Usage: sdd-modeler migrate <from-hash|name:version> <to-hash|name:version> [options]
 *
 * <p>Examples:
 * <ul>
 *   <li>sdd-modeler migrate orders:1.0 orders:2.0
 *   <li>sdd-modeler migrate abc123 def456 --dialect postgres
 *   <li>sdd-modeler migrate orders:1.0 orders:2.0 -o migration.sql
 *   <li>sdd-modeler migrate abc123 def456 --llm ollama --model llama3.2
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

    @Override
    public Integer call() {
        // Validate dialect
        if (!isValidDialect(dialect)) {
            logger.error("ERROR: Unsupported dialect '{}'", dialect);
            logger.error("  Supported dialects: postgres");
            return 1;
        }

        return io.vavr.control.Try.of(() -> {
                    try (var repository = repositoryMixin.createRepository()) {
                        // Find source SDR
                        var fromResult = findSdr(repository, fromIdentifier);
                        if (fromResult.isFailure()) {
                            throw new IllegalStateException("Failed to retrieve source SDR: "
                                    + fromResult.getCause().getMessage());
                        }
                        var fromSdrOpt = fromResult.get();
                        if (fromSdrOpt.isEmpty()) {
                            throw new IllegalStateException("Source SDR not found: " + fromIdentifier);
                        }
                        SdrRecord fromSdr = fromSdrOpt.get();

                        // Find target SDR
                        var toResult = findSdr(repository, toIdentifier);
                        if (toResult.isFailure()) {
                            throw new IllegalStateException("Failed to retrieve target SDR: "
                                    + toResult.getCause().getMessage());
                        }
                        var toSdrOpt = toResult.get();
                        if (toSdrOpt.isEmpty()) {
                            throw new IllegalStateException("Target SDR not found: " + toIdentifier);
                        }
                        SdrRecord toSdr = toSdrOpt.get();

                        // Check if migration already exists
                        if (!force) {
                            var existingResult = repository.findMigration(fromSdr.schemaHash(), toSdr.schemaHash());
                            if (existingResult.isSuccess()
                                    && existingResult.get().isPresent()) {
                                logger.info("INFO: Migration already exists (use --force to regenerate)");
                                var migration = existingResult.get().get();
                                outputMigration(migration.migrationScript());
                                if (outputJson != null) {
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    var dto = new MigrationDto(
                                            migration.confidence(), migration.comments(), migration.migrationScript());
                                    objectMapper.writeValue(outputJson, dto);
                                    logger.info("  JSON Output: {}", outputJson.getAbsolutePath());
                                }
                                return 0;
                            }
                        }

                        // Create LLM-based migration service
                        logger.info("INFO: Generating migration using {} LLM...", llmProvider);
                        ChatModel llmModel = null;
                        llmModel =
                                io.vavr.control.Try.of(() -> createLlmModel()).get();

                        var migrationGenerator = new LangChainMigrationGenerationService(llmModel);
                        var comparisonService = new DdlComparisonService();
                        var orchestrationService =
                                new MigrationOrchestrationService(migrationGenerator, comparisonService, repository);

                        // Generate and save migration
                        var migrationResult = orchestrationService.generateAndSaveMigration(fromSdr, toSdr, dialect);
                        if (migrationResult.isFailure()) {
                            throw new IllegalStateException("Failed to generate migration: "
                                    + migrationResult.getCause().getMessage());
                        }

                        var migration = migrationResult.get();
                        logger.info("SUCCESS: Migration generated and saved");
                        logger.info("  From: {}", fromSdr.schemaHash());
                        logger.info("  To:   {}", toSdr.schemaHash());
                        logger.info("  Dialect: {}", dialect);

                        // Output migration script
                        outputMigration(migration.migrationScript());

                        if (outputJson != null) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            var dto = new MigrationDto(
                                    migration.confidence(), migration.comments(), migration.migrationScript());
                            objectMapper.writeValue(outputJson, dto);
                            logger.info("  JSON Output: {}", outputJson.getAbsolutePath());
                        }

                        return 0;
                    }
                })
                .fold(
                        throwable -> {
                            if (throwable instanceof NoClassDefFoundError) {
                                logger.error("ERROR: LangChain4j dependencies not found");
                                logger.error("  The 'migrate' command requires LangChain4j libraries.");
                                logger.error("  Please ensure the following dependencies are available:");
                                logger.error("    - dev.langchain4j:langchain4j:0.36.2");
                                logger.error("    - dev.langchain4j:langchain4j-ollama:0.36.2");
                                logger.error("    - dev.langchain4j:langchain4j-openai:0.36.2 (if using --llm openai)");
                                logger.error("  Missing class: {}", throwable.getMessage());
                                return 1;
                            }
                            logger.error("ERROR: Unexpected error");
                            logger.error("  {}", throwable.getMessage());
                            throwable.printStackTrace();
                            return 1;
                        },
                        result -> result);
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
            var writeResult = io.vavr.control.Try.of(() -> Files.writeString(outputFile.toPath(), script));
            writeResult.onFailure(e -> logger.warn("WARN: Failed to write output: {}", e.getMessage()));
            if (writeResult.isFailure()) {
                System.out.println(script);
            } else {
                logger.info("  Output: {}", outputFile.getAbsolutePath());
            }
        } else {
            System.out.println(script);
        }
    }
}
