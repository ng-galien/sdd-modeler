package io.statemodeler.cli;

import io.statemodeler.diagram.DiagramGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to generate state diagrams from an SDD model file.
 */
@Command(name = "diagram", description = "Generate state diagrams from an SDD model file (Mermaid, PlantUML)")
public class DiagramCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Option(
            names = {"-f", "--format"},
            description = "Diagram format: mermaid (default: mermaid)",
            defaultValue = "mermaid")
    private String format;

    @Option(
            names = {"-o", "--output"},
            description = "Output file path (default: stdout)")
    private Path outputFile;

    @Option(
            names = {"-e", "--entity"},
            description = "Generate diagram for specific entity only")
    private String entityName;

    @Override
    public Integer call() throws Exception {
        System.out.println("Generating diagram from model file: " + modelFile);

        try {
            // Check if model file exists
            if (!Files.exists(modelFile)) {
                System.err.println("Error: Model file does not exist: " + modelFile);
                return 1;
            }

            // Validate format
            if (!DiagramGenerators.isSupported(format)) {
                System.err.println("Error: Unsupported diagram format: " + format);
                System.err.println("Supported formats: " + String.join(", ", DiagramGenerators.getSupportedFormats()));
                return 1;
            }

            // Load model
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

            // Validate entity name if specified
            if (entityName != null && !model.entities().containsKey(entityName)) {
                System.err.println("Error: Entity not found: " + entityName);
                System.err.println("Available entities: "
                        + String.join(", ", model.entities().keySet()));
                return 1;
            }

            // Generate diagram
            var generator = DiagramGenerators.forFormat(format);
            String diagram;

            if (entityName != null) {
                System.out.println("Generating diagram for entity: " + entityName);
                diagram = generator.generateDiagram(model, entityName);
            } else {
                System.out.println("Generating diagram for all entities");
                diagram = generator.generateDiagram(model);
            }

            // Write output
            if (outputFile != null) {
                Files.writeString(outputFile, diagram);
                System.out.println("✓ Diagram written to: " + outputFile);
            } else {
                System.out.println("\n" + diagram);
            }

            return 0;

        } catch (IOException e) {
            System.err.println("Error: Failed to read/write file: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
