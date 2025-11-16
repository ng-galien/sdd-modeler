package io.statemodeler.cli;

import io.statemodeler.diagram.DiagramGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to generate state diagrams from an SDD model file.
 */
@Command(name = "diagram", description = "Generate state diagrams from an SDD model file (Mermaid, PlantUML)")
public class DiagramCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(DiagramCommand.class);

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
        logger.info("Generating diagram from model file: {}", modelFile);

        try {
            // Check if model file exists
            if (!Files.exists(modelFile)) {
                logger.error("Error: Model file does not exist: {}", modelFile);
                return 1;
            }

            // Validate format
            if (!DiagramGenerators.isSupported(format)) {
                logger.error("Error: Unsupported diagram format: {}", format);
                logger.error("Supported formats: {}", String.join(", ", DiagramGenerators.getSupportedFormats()));
                return 1;
            }

            // Load model
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

            // Validate entity name if specified
            if (entityName != null && !model.entities().containsKey(entityName)) {
                logger.error("Error: Entity not found: {}", entityName);
                logger.error(
                        "Available entities: {}",
                        String.join(", ", model.entities().keySet()));
                return 1;
            }

            // Generate diagram
            var generator = DiagramGenerators.forFormat(format);
            String diagram;

            if (entityName != null) {
                logger.info("Generating diagram for entity: {}", entityName);
                diagram = generator.generateDiagram(model, entityName);
            } else {
                logger.info("Generating diagram for all entities");
                diagram = generator.generateDiagram(model);
            }

            // Write output
            if (outputFile != null) {
                Files.writeString(outputFile, diagram);
                logger.info("✓ Diagram written to: {}", outputFile);
            } else {
                System.out.println("\n" + diagram);
            }

            return 0;

        } catch (IOException e) {
            logger.error("Error: Failed to read/write file: {}", e.getMessage());
            return 1;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            return 1;
        }
    }
}
