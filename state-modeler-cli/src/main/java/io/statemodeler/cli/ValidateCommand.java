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
    public Integer call() throws Exception {
        logger.info("Validating model file: {}", modelFile);

        try {
            // Check if model file exists
            if (!Files.exists(modelFile)) {
                logger.error("Error: Model file does not exist: {}", modelFile);
                return 1;
            }

            // Load model using appropriate loader based on file extension
            var loader = ModelLoader.forFile(modelFile);
            var loadResult = loader.loadFromFile(modelFile);

            if (loadResult.isFailure()) {
                logger.error("✗ Failed to parse model file:");
                logger.error("  {}", loadResult.getCause().getMessage());
                return 1;
            }

            var model = loadResult.get();
            logger.info("✓ Model parsed successfully: {}", model.name());

            // Validate the loaded model
            var validator = ModelValidators.getInstance();
            var validationResult = validator.validate(model);

            if (validationResult.isInvalid()) {
                logger.error("✗ Model validation failed:");
                for (var error : validationResult.getError()) {
                    logger.error("  • {}", error.message());
                }
                return 1;
            }

            logger.info("✓ Model validation passed");
            return 0;

        } catch (Exception e) {
            logger.error("Error validating model: {}", e.getMessage());
            return 1;
        }
    }
}
