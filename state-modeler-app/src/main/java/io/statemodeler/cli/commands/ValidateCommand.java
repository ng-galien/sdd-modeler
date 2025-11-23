package io.statemodeler.cli.commands;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import io.statemodeler.cli.util.PathUtils;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Command to validate an SDD model file.
 */
@Command(name = "validate", description = "Validate an SDD model file for syntax and semantic correctness")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Override
    public Integer call() {
        var resolvedModelFile = PathUtils.resolveFromProcess(modelFile);
        logger.info("Validating model file: {}", resolvedModelFile);
        if (!Files.exists(resolvedModelFile)) {
            spec.commandLine().getErr().println("Error: Model file does not exist: " + resolvedModelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                // create loader and load model
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(resolvedModelFile))
                // handle load/validate result
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
                            spec.commandLine().getOut().println("✓ Model validation passed");
                            return 0;
                        });
    }
}
