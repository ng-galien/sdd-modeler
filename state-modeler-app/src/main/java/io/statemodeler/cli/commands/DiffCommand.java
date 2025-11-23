package io.statemodeler.cli.commands;

import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import io.statemodeler.cli.util.PathUtils;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Command to compare DDL between two SDD model files and display the diff.
 */
@Command(name = "diff", description = "Compare DDL between two SDD model files")
public class DiffCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(DiffCommand.class);

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Path to the current SDD model file (YAML or JSON)")
    private Path currentModelFile;

    @Parameters(index = "1", description = "Path to the future SDD model file (YAML or JSON)")
    private Path futureModelFile;

    @Option(
            names = {"--dialect"},
            description = "SQL dialect (supported: postgres)",
            defaultValue = "postgres")
    private String dialect;

    @Override
    public Integer call() {
        var resolvedCurrent = PathUtils.resolveFromProcess(currentModelFile);
        var resolvedFuture = PathUtils.resolveFromProcess(futureModelFile);
        logger.info("Comparing DDL between:");
        logger.info("  Current: {}", resolvedCurrent);
        logger.info("  Future:  {}", resolvedFuture);
        logger.info("  Dialect: {}", dialect);

        // Validate dialect and file existence to keep error messages stable
        if (!DdlGenerators.isSupported(dialect)) {
            spec.commandLine().getErr().println("Error: Unsupported SQL dialect '" + dialect + "'");
            spec.commandLine()
                    .getErr()
                    .println("Supported dialects: " + String.join(", ", DdlGenerators.getSupportedDialects()));
            return 1;
        }
        if (!Files.exists(resolvedCurrent)) {
            spec.commandLine().getErr().println("Error: Current model file does not exist: " + resolvedCurrent);
            return 1;
        }
        if (!Files.exists(resolvedFuture)) {
            spec.commandLine().getErr().println("Error: Future model file does not exist: " + resolvedFuture);
            return 1;
        }

        return io.vavr.control.Try.of(() -> {
                    var currentLoader = ModelLoader.forFile(resolvedCurrent);
                    var currentLoadResult = currentLoader.loadFromFile(resolvedCurrent);
                    if (currentLoadResult.isFailure()) {
                        spec.commandLine().getErr().println("✗ Failed to parse current model file:");
                        spec.commandLine()
                                .getErr()
                                .println("  " + currentLoadResult.getCause().getMessage());
                        return 1;
                    }
                    var currentModel = currentLoadResult.get();
                    spec.commandLine().getOut().println("✓ Current model parsed: " + currentModel.name());
                    var validator = ModelValidators.getInstance();
                    var currentValidation = validator.validate(currentModel);
                    if (currentValidation.isInvalid()) {
                        throw new IllegalArgumentException("Current model validation failed");
                    }

                    var futureLoader = ModelLoader.forFile(resolvedFuture);
                    var futureLoadResult = futureLoader.loadFromFile(resolvedFuture);
                    if (futureLoadResult.isFailure()) {
                        spec.commandLine().getErr().println("✗ Failed to parse future model file:");
                        spec.commandLine()
                                .getErr()
                                .println("  " + futureLoadResult.getCause().getMessage());
                        return 1;
                    }
                    var futureModel = futureLoadResult.get();
                    spec.commandLine().getOut().println("✓ Future model parsed: " + futureModel.name());
                    var futureValidation = validator.validate(futureModel);
                    if (futureValidation.isInvalid()) {
                        throw new IllegalArgumentException("Future model validation failed");
                    }

                    var generator = DdlGenerators.forDialect(dialect);
                    var currentStatements = generator.generateDdl(currentModel);
                    var currentDdl = String.join(";\n", currentStatements) + ";\n";
                    logger.info("✓ Current DDL generated ({} chars)", currentDdl.length());
                    var futureStatements = generator.generateDdl(futureModel);
                    var futureDdl = String.join(";\n", futureStatements) + ";\n";
                    logger.info("✓ Future DDL generated ({} chars)", futureDdl.length());

                    var comparisonService = new DdlComparisonService();
                    var comparison = comparisonService.compare(currentDdl, futureDdl);
                    if (comparison.diff().isEmpty()) {
                        spec.commandLine().getOut().println("");
                        spec.commandLine().getOut().println("✓ No differences found - DDL schemas are identical");
                        return 0;
                    }

                    spec.commandLine().getOut().println("");
                    spec.commandLine().getOut().println("DDL Diff:");
                    spec.commandLine().getOut().println("─".repeat(80));
                    for (var line : comparison.diff()) {
                        spec.commandLine().getOut().println(line);
                    }
                    spec.commandLine().getOut().println("─".repeat(80));
                    logger.info("✓ Diff generated ({} lines)", comparison.diff().size());
                    return 0;
                })
                .fold(
                        throwable -> {
                            spec.commandLine()
                                    .getErr()
                                    .println("Error during DDL comparison: " + throwable.getMessage());
                            return 1;
                        },
                        result -> result);
    }
}
