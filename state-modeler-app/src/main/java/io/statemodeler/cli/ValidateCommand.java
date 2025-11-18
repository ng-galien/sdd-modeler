package io.statemodeler.cli;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to validate an SDD model file.
 */
@Command(name = "validate", description = "Validate an SDD model file for syntax and semantic correctness")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Override
    public Integer call() {
        logger.info("Validating model file: {}", modelFile);
        if (!Files.exists(modelFile)) {
            logger.error("Error: Model file does not exist: {}", modelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                // create loader and load model
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(modelFile))
                // handle load/validate result
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
                            logger.info("✓ Model validation passed");
                            return 0;
                        });
    }
}
