package io.statemodeler.cli;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.validation.ModelValidators;
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

            // Load and validate the model
            var loader = ModelLoader.forFile(modelFile);
            var loadResult = loader.loadFromFile(modelFile);

            if (loadResult.isFailure()) {
                System.err.println("✗ Failed to parse model file:");
                System.err.println("  " + loadResult.getCause().getMessage());
                return 1;
            }

            var model = loadResult.get();
            System.out.println("✓ Model parsed successfully: " + model.name());

            // Validate the model before generating SQL
            var validator = ModelValidators.getInstance();
            var validationResult = validator.validate(model);

            if (validationResult.isInvalid()) {
                System.err.println("✗ Model validation failed:");
                for (var error : validationResult.getError()) {
                    System.err.println("  • " + error.message());
                }
                return 1;
            }

            // Generate DDL
            var generator = DdlGenerators.forDialect(dialect);
            var ddlStatements = generator.generateDdl(model);

            var output = String.join(";\n", ddlStatements) + ";\n";

            // Write to file or stdout
            if (outputFile != null) {
                Files.writeString(outputFile, output);
                System.out.println("✓ DDL written to: " + outputFile);
            } else {
                System.out.println();
                System.out.println("-- Generated DDL for " + model.name());
                System.out.println(output);
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error generating SQL: " + e.getMessage());
            return 1;
        }
    }
}
