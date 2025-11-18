package io.statemodeler.cli;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to generate SQL DDL from an SDD model file.
 */
@Command(name = "sql", description = "Generate SQL DDL from an SDD model file")
public class SqlCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SqlCommand.class);

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
    public Integer call() {
        logger.info("Generating SQL for model file: {}", modelFile);
        logger.info("Dialect: {}", dialect);

        if (!DdlGenerators.isSupported(dialect)) {
            logger.error("Error: Unsupported SQL dialect '{}'", dialect);
            logger.error("Supported dialects: {}", String.join(", ", DdlGenerators.getSupportedDialects()));
            return 1;
        }
         // Check if model file exists
        if (!Files.exists(modelFile)) {
            logger.error("Error: Model file does not exist: {}", modelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(modelFile))
                .fold(
                        throwable -> {
                            logger.error("✗ Failed to parse model file:");
                            logger.error("  {}", throwable.getMessage());
                            return 1;
                        },
                        model -> {
                            logger.info("✓ Model parsed successfully: {}", model.name());
                            var validationResult = ModelValidators.getInstance().validate(model);
                            if (validationResult.isInvalid()) {
                                logger.error("✗ Model validation failed:");
                                for (var error : validationResult.getError()) {
                                    logger.error("  • {}", error.message());
                                }
                                return 1;
                            }

                            // Generate DDL
                            var generator = DdlGenerators.forDialect(dialect);
                            var ddlStatements = generator.generateDdl(model);
                            var output = String.join(";\n", ddlStatements) + ";\n";

                            // Write to file or stdout
                            if (outputFile != null) {
                                var writeResult = io.vavr.control.Try.of(() -> Files.writeString(outputFile, output));
                                if (writeResult.isFailure()) {
                                    logger.error(
                                            "Error writing DDL output: {}",
                                            writeResult.getCause().getMessage());
                                    return 1;
                                }
                                logger.info("✓ DDL written to: {}", outputFile);
                            } else {
                                System.out.println();
                                System.out.println("-- Generated DDL for " + model.name());
                                System.out.println(output);
                            }

                            return 0;
                        });
    }
}
