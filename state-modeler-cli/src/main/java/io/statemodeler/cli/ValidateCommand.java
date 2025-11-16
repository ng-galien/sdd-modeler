package io.statemodeler.cli;

import io.statemodeler.loader.ModelLoaders;
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
            // Check if file exists
            if (!Files.exists(modelFile)) {
                System.err.println("Error: Model file does not exist: " + modelFile);
                return 1;
            }

            // Load model using appropriate ModelLoader based on file extension
            var loader = ModelLoaders.forFile(modelFile);
            var model = loader.loadFromFile(modelFile);

            // Basic validation checks
            if (model.entities().isEmpty()) {
                System.out.println("⚠️  Warning: Model contains no entities");
            }

            // Report success
            System.out.println("✓ Model loaded successfully");
            System.out.println("  - Version: " + model.version());
            System.out.println("  - Name: " + model.name());
            System.out.println("  - Dialect: " + model.database().dialect());
            System.out.println("  - Entities: " + model.entities().size());

            // TODO: Add comprehensive validation rules once ModelValidator is implemented
            // For now, successful parsing means basic validation passed

            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error validating model: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            return 1;
        }
    }
}
