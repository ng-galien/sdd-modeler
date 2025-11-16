package io.statemodeler.cli;

import io.statemodeler.loader.ModelLoaders;
import io.statemodeler.sql.DdlGenerators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to generate SQL DDL from an SDD model file.
 */
@Command(name = "sql", description = "Generate SQL DDL from an SDD model file")
public class SqlCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Option(
            names = {"--dialect"},
            description = "SQL dialect (supported: postgres)",
            defaultValue = "postgres")
    private String dialect;

    @Option(
            names = {"-o", "--output"},
            description = "Output file (default: stdout)")
    private Path outputFile;

    @Override
    public Integer call() throws Exception {
        try {
            // Check if dialect is supported
            if (!DdlGenerators.isSupported(dialect)) {
                System.err.println("Error: Unsupported SQL dialect '" + dialect + "'");
                System.err.println("Supported dialects: " + String.join(", ", DdlGenerators.getSupportedDialects()));
                return 1;
            }

            // Check if model file exists
            if (!Files.exists(modelFile)) {
                System.err.println("Error: Model file does not exist: " + modelFile);
                return 1;
            }

            // Load model using appropriate ModelLoader
            System.err.println("Loading model from: " + modelFile);
            var loader = ModelLoaders.forFile(modelFile);
            var model = loader.loadFromFile(modelFile);

            System.err.println("✓ Model loaded successfully");
            System.err.println("  - Name: " + model.name());
            System.err.println("  - Entities: " + model.entities().size());

            // Generate DDL
            System.err.println("\nGenerating DDL for dialect: " + dialect);
            var generator = DdlGenerators.forDialect(dialect);
            var ddl = generator.generateDdl(model);

            // Output DDL
            if (outputFile != null) {
                Files.writeString(outputFile, ddl, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.err.println("✓ DDL written to: " + outputFile);
            } else {
                // Output to stdout
                System.out.println(ddl);
            }

            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error generating SQL: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            return 1;
        }
    }
}
