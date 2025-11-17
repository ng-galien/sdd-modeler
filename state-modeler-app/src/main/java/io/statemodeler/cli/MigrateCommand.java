package io.statemodeler.cli;

import dev.langchain4j.model.chat.ChatLanguageModel;
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
            names = {"--llm"},
            description = "LLM provider: jlama (default), ollama",
            defaultValue = "jlama")
    String llmProvider;

    @Option(
            names = {"--model"},
            description = "LLM model name (default: TinyLlama-1.1B-Chat-v1.0-Jlama-Q4 for jlama, llama3.2 for ollama)")
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
            System.err.println("ERROR: Unsupported dialect '" + dialect + "'");
            System.err.println("  Supported dialects: postgres");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            // Find source SDR
            var fromResult = findSdr(repository, fromIdentifier);
            if (fromResult.isFailure()) {
                System.err.println("ERROR: Failed to retrieve source SDR");
                System.err.println("  " + fromResult.getCause().getMessage());
                return 1;
            }
            var fromSdrOpt = fromResult.get();
            if (fromSdrOpt.isEmpty()) {
                System.err.println("ERROR: Source SDR not found: " + fromIdentifier);
                return 1;
            }
            SdrRecord fromSdr = fromSdrOpt.get();

            // Find target SDR
            var toResult = findSdr(repository, toIdentifier);
            if (toResult.isFailure()) {
                System.err.println("ERROR: Failed to retrieve target SDR");
                System.err.println("  " + toResult.getCause().getMessage());
                return 1;
            }
            var toSdrOpt = toResult.get();
            if (toSdrOpt.isEmpty()) {
                System.err.println("ERROR: Target SDR not found: " + toIdentifier);
                return 1;
            }
            SdrRecord toSdr = toSdrOpt.get();

            // Check if migration already exists
            if (!force) {
                var existingResult = repository.findMigration(fromSdr.schemaHash(), toSdr.schemaHash());
                if (existingResult.isSuccess() && existingResult.get().isPresent()) {
                    System.err.println("INFO: Migration already exists (use --force to regenerate)");
                    var migration = existingResult.get().get();
                    outputMigration(migration.migrationScript());
                    return 0;
                }
            }

            // Create LLM-based migration service
            System.err.println("INFO: Generating migration using " + llmProvider + " LLM...");

            ChatLanguageModel llmModel;
            try {
                llmModel = createLlmModel();
            } catch (NoClassDefFoundError e) {
                System.err.println("ERROR: LangChain4j dependencies not found");
                System.err.println("  The 'migrate' command requires LangChain4j libraries.");
                System.err.println("  Please ensure the following dependencies are available:");
                System.err.println("    - dev.langchain4j:langchain4j:0.36.2");
                System.err.println("    - dev.langchain4j:langchain4j-jlama:0.36.2 (for jlama provider)");
                System.err.println("    - dev.langchain4j:langchain4j-ollama:0.36.2 (for ollama provider)");
                System.err.println("  Missing class: " + e.getMessage());
                return 1;
            }

            var migrationGenerator = new LangChainMigrationGenerationService(llmModel);
            var comparisonService = new DdlComparisonService();
            var orchestrationService =
                    new MigrationOrchestrationService(migrationGenerator, comparisonService, repository);

            // Generate and save migration
            var migrationResult = orchestrationService.generateAndSaveMigration(fromSdr, toSdr, dialect);

            if (migrationResult.isFailure()) {
                System.err.println("ERROR: Failed to generate migration");
                System.err.println("  " + migrationResult.getCause().getMessage());
                return 1;
            }

            var migration = migrationResult.get();
            System.err.println("SUCCESS: Migration generated and saved");
            System.err.println("  From: " + fromSdr.schemaHash());
            System.err.println("  To:   " + toSdr.schemaHash());
            System.err.println("  Dialect: " + dialect);

            // Output migration script
            outputMigration(migration.migrationScript());

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
            return 1;
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
            // Find latest version by name
            return repository.findByName(name).map(metadataList -> {
                if (metadataList.isEmpty()) {
                    return Optional.empty();
                }
                // Get most recent (first in DESC order)
                String hash = metadataList.get(0).schemaHash();
                return repository.findByHash(hash).get();
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
    private ChatLanguageModel createLlmModel() {
        String effectiveModelName = modelName != null ? modelName : getDefaultModelName();
        ChatModelProvider provider = new LangChainModelProvider();

        // Only Ollama supported now
        return provider.createModel(effectiveModelName, 0.7, "http://localhost:11434", 300);
    }

    /**
     * Get default model name for provider.
     */
    private String getDefaultModelName() {
        return switch (llmProvider.toLowerCase()) {
            case "jlama" -> "tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4";
            case "ollama" -> "llama3.2";
            default -> "tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4";
        };
    }

    /**
     * Output migration script to file or stdout.
     */
    private void outputMigration(String script) {
        try {
            if (outputFile != null) {
                Files.writeString(outputFile.toPath(), script);
                System.err.println("  Output: " + outputFile.getAbsolutePath());
            } else {
                System.out.println(script);
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to write output: " + e.getMessage());
            System.out.println(script);
        }
    }
}
