package io.statemodeler.cli;

import io.statemodeler.diagram.DiagramGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
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
    public Integer call() {
        logger.info("Generating diagram from model file: {}", modelFile);

        // Validate file existence and format to keep stable error messages
        if (!Files.exists(modelFile)) {
            logger.error("Error: Model file does not exist: {}", modelFile);
            return 1;
        }
        if (!DiagramGenerators.isSupported(format)) {
            logger.error("Error: Unsupported diagram format: {}", format);
            logger.error("Supported formats: {}", String.join(", ", DiagramGenerators.getSupportedFormats()));
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(modelFile))
                .fold(
                        throwable -> {
                            logger.error("✗ Failed to parse model file:");
                            logger.error("  {}", throwable.getMessage());
                            return 1;
                        },
                        model -> {
                            logger.info("✓ Model parsed successfully: {}", model.name());
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

                            if (entityName != null && !model.entities().containsKey(entityName)) {
                                logger.error("Error: Entity not found: {}", entityName);
                                logger.error(
                                        "Available entities: {}",
                                        String.join(", ", model.entities().keySet()));
                                return 1;
                            }

                            var generator = DiagramGenerators.forFormat(format);
                            String diagram = entityName != null
                                    ? generator.generateDiagram(model, entityName)
                                    : generator.generateDiagram(model);

                            return io.vavr.control.Try.of(() -> {
                                        if (outputFile != null) {
                                            Files.writeString(outputFile, diagram);
                                            logger.info("✓ Diagram written to: {}", outputFile);
                                        } else {
                                            System.out.println("\n" + diagram);
                                        }
                                        return 0;
                                    })
                                    .fold(
                                            e -> {
                                                logger.error("Error: Failed to read/write file: {}", e.getMessage());
                                                return 1;
                                            },
                                            result -> result);
                        });
    }
}
