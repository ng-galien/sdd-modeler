package io.statemodeler.cli.commands;

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
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Command to generate SQL DDL from an SDD model file.
 */
@Command(name = "sql", description = "Generate SQL DDL from an SDD model file")
public class SqlCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SqlCommand.class);

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Option(names = { "--dialect" }, description = "SQL dialect (supported: postgres)", defaultValue = "postgres")
    private String dialect;

    @Option(names = { "-o", "--output" }, description = "Output file (default: stdout)")
    private Path outputFile;

    @Override
    public Integer call() {
        logger.info("Generating SQL for model file: {}", modelFile);
        logger.info("Dialect: {}", dialect);

        if (!DdlGenerators.isSupported(dialect)) {
            spec.commandLine().getErr().println("Error: Unsupported SQL dialect '" + dialect + "'");
            spec.commandLine().getErr()
                    .println("Supported dialects: " + String.join(", ", DdlGenerators.getSupportedDialects()));
            return 1;
        }
        // Check if model file exists
        if (!Files.exists(modelFile)) {
            spec.commandLine().getErr().println("Error: Model file does not exist: " + modelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(modelFile))
                .fold(
                        throwable -> {
                            spec.commandLine().getErr().println("✗ Failed to parse model file:");
                            spec.commandLine().getErr().println("  " + throwable.getMessage());
                            return 1;
                        },
                        model -> {
                            spec.commandLine().getOut().println("✓ Model parsed successfully: " + model.name());
                            var validationResult = ModelValidators.getInstance().validate(model);
                            if (validationResult.isInvalid()) {
                                spec.commandLine().getErr().println("✗ Model validation failed:");
                                for (var error : validationResult.getError()) {
                                    spec.commandLine().getErr().println("  • " + error.message());
                                }
                                return 1;
                            }

                            // Generate DDL
                            var generator = DdlGenerators.forDialect(dialect);
                            var ddlStatements = generator.generateDdl(model);
                            var ddlContent = String.join(";\n", ddlStatements) + ";\n";

                            // Write to file or stdout
                            if (outputFile != null) {
                                var writeResult = io.vavr.control.Try
                                        .of(() -> Files.writeString(outputFile, ddlContent));
                                if (writeResult.isFailure()) {
                                    logger.error(
                                            "Error writing DDL output: {}",
                                            writeResult.getCause().getMessage());
                                    return 1;
                                }
                                logger.info("✓ DDL written to: {}", outputFile);
                            } else {
                                spec.commandLine().getOut().println();
                                spec.commandLine().getOut().println("-- Generated DDL for " + model.name());
                                spec.commandLine().getOut().println(ddlContent);
                            }

                            return 0;
                        });
    }
}
