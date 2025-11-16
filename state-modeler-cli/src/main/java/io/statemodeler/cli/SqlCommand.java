package io.statemodeler.cli;

import io.statemodeler.sql.DdlGenerators;
import java.nio.file.Files;
import java.nio.file.Path;
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
        System.out.println("Generating SQL for model file: " + modelFile);
        System.out.println("Dialect: " + dialect);

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

            // For now, generate a simple example DDL to show the integration
            var generator = DdlGenerators.forDialect(dialect);

            // TODO: Implement model loading from YAML/JSON
            // For now, show that the DDL generator works
            System.out.println("DDL Generator initialized successfully for dialect: " + generator.getDialect());
            System.out.println("✓ Model parsing from YAML/JSON not yet implemented");
            System.out.println("✓ Once YAML loader is ready, full DDL generation will work");

            if (outputFile != null) {
                System.out.println("Will output to: " + outputFile);
            } else {
                System.out.println("Will output to: stdout");
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error generating SQL: " + e.getMessage());
            return 1;
        }
    }
}
