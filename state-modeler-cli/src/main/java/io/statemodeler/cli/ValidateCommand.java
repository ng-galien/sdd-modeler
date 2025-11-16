package io.statemodeler.cli;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to validate an SDD model file.
 */
@Command(name = "validate", description = "Validate an SDD model file for syntax and semantic correctness")
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Override
    public Integer call() throws Exception {
        System.out.println("Validating model file: " + modelFile);

        try {
            // Check if model file exists
            if (!Files.exists(modelFile)) {
                System.err.println("Error: Model file does not exist: " + modelFile);
                return 1;
            }

            // Load model using appropriate loader based on file extension
            var loader = ModelLoader.forFile(modelFile);
            var loadResult = loader.loadFromFile(modelFile);

            if (loadResult.isFailure()) {
                System.err.println("✗ Failed to parse model file:");
                System.err.println("  " + loadResult.getCause().getMessage());
                return 1;
            }

            var model = loadResult.get();
            System.out.println("✓ Model parsed successfully: " + model.name());

            // Validate the loaded model
            var validator = ModelValidators.getInstance();
            var validationResult = validator.validate(model);

            if (validationResult.isInvalid()) {
                System.err.println("✗ Model validation failed:");
                for (var error : validationResult.getError()) {
                    System.err.println("  • " + error.message());
                }
                return 1;
            }

            System.out.println("✓ Model validation passed");
            return 0;

        } catch (Exception e) {
            System.err.println("Error validating model: " + e.getMessage());
            return 1;
        }
    }
}
