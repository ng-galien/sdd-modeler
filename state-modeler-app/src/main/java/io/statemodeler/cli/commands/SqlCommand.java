package io.statemodeler.cli.commands;

import io.statemodeler.cli.util.PathUtils;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
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

    @Option(
            names = {"--dialect"},
            description = "SQL dialect (supported: postgres)",
            defaultValue = "postgres")
    private String dialect;

    @Option(
            names = {"-o", "--output"},
            description = "Output file (default: stdout)")
    private Path outputFile;

    @Option(
            names = {"--format-ddl"},
            description = "Format the generated DDL for better readability",
            defaultValue = "false")
    private boolean formatDdl;

    @Override
    public Integer call() {
        logger.info("Generating SQL for model file: {}", modelFile);
        logger.info("Dialect: {}", dialect);

        if (!DdlGenerators.isSupported(dialect)) {
            spec.commandLine().getErr().println("Error: Unsupported SQL dialect '" + dialect + "'");
            spec.commandLine()
                    .getErr()
                    .println("Supported dialects: " + String.join(", ", DdlGenerators.getSupportedDialects()));
            return 1;
        }
        // Resolve paths with helper relative to current process
        var resolvedModelFile = PathUtils.resolveFromProcess(modelFile);
        var resolvedOutputFile = outputFile == null ? null : PathUtils.resolveFromProcess(outputFile);

        // Check if model file exists
        if (!Files.exists(resolvedModelFile)) {
            spec.commandLine().getErr().println("Error: Model file does not exist: " + modelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(resolvedModelFile))
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
                            var ddlContent =
                                    formatDdl ? generator.generateFormattedDdl(model) : generator.generateDdl(model);

                            // Write to file or stdout
                            if (resolvedOutputFile != null) {
                                try {
                                    PathUtils.ensureParentDirectoryExists(resolvedOutputFile);
                                } catch (Exception e) {
                                    logger.error(
                                            "Failed to create directory {}: {}",
                                            resolvedOutputFile.getParent(),
                                            e.getMessage());
                                    spec.commandLine()
                                            .getErr()
                                            .println("Error: Could not create output directory: "
                                                    + resolvedOutputFile.getParent());
                                    return 1;
                                }

                                var writeResult =
                                        io.vavr.control.Try.of(() -> Files.writeString(resolvedOutputFile, ddlContent));
                                if (writeResult.isFailure()) {
                                    logger.error(
                                            "Error writing DDL output: {}",
                                            writeResult.getCause().getMessage());
                                    spec.commandLine()
                                            .getErr()
                                            .println("Error: Could not write DDL to file: " + outputFile);
                                    return 1;
                                }
                                logger.info("✓ DDL written to: {}", resolvedOutputFile);
                            } else {
                                spec.commandLine().getOut().println();
                                spec.commandLine().getOut().println("-- Generated DDL for " + model.name());
                                spec.commandLine().getOut().println(ddlContent);
                            }

                            return 0;
                        });
    }
}
