package io.statemodeler.cli.commands;

import io.statemodeler.diagram.DiagramGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import io.vavr.control.Try;
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
 * Command to generate state diagrams from an SDD model file.
 */
@Command(name = "diagram", description = "Generate state diagrams from an SDD model file (Mermaid, PlantUML)")
public class DiagramCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(DiagramCommand.class);

    @Spec
    CommandSpec spec;

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
        // Resolve model path from process and localize output path
        var resolvedModelFile = PathUtils.resolveFromProcess(modelFile);
        var resolvedOutputFile = outputFile == null ? null : PathUtils.resolveFromProcess(outputFile);
        logger.info("Generating diagram from model file: {}", resolvedModelFile);

        // Validate file existence and format to keep stable error messages
        if (!Files.exists(resolvedModelFile)) {
            spec.commandLine().getErr().println("Error: Model file does not exist: " + resolvedModelFile);
            return 1;
        }
        if (!DiagramGenerators.isSupported(format)) {
            spec.commandLine().getErr().println("Error: Unsupported diagram format: " + format);
            spec.commandLine()
                    .getErr()
                    .println("Supported formats: " + String.join(", ", DiagramGenerators.getSupportedFormats()));
            return 1;
        }

        return Try.of(() -> modelFile)
                .map(ModelLoader::forFile)
                .flatMap(loader -> loader.loadFromFile(modelFile))
                .fold(
                        throwable -> {
                            spec.commandLine().getErr().println("✗ Failed to parse model file:");
                            spec.commandLine().getErr().println("  " + throwable.getMessage());
                            return 1;
                        },
                        model -> {
                            spec.commandLine().getOut().println("✓ Model parsed successfully: " + model.name());
                            var validator = ModelValidators.getInstance();
                            var validationResult = validator.validate(model);
                            if (validationResult.isInvalid()) {
                                spec.commandLine().getErr().println("✗ Model validation failed:");
                                for (var error : validationResult.getError()) {
                                    spec.commandLine().getErr().println("  • " + error.message());
                                }
                                return 1;
                            }
                            spec.commandLine().getOut().println("✓ Model validation passed");

                            if (entityName != null && !model.entities().containsKey(entityName)) {
                                spec.commandLine().getErr().println("Error: Entity not found: " + entityName);
                                spec.commandLine()
                                        .getErr()
                                        .println("Available entities: "
                                                + String.join(
                                                        ", ", model.entities().keySet()));
                                return 1;
                            }

                            var generator = DiagramGenerators.forFormat(format);
                            String diagram = entityName != null
                                    ? generator.generateDiagram(model, entityName)
                                    : generator.generateDiagram(model);

                            return io.vavr.control.Try.of(() -> {
                                        if (resolvedOutputFile != null) {
                                            try {
                                                PathUtils.ensureParentDirectoryExists(resolvedOutputFile);
                                            } catch (Exception e) {
                                                spec.commandLine().getErr().println("Error: Could not create output directory: " + e.getMessage());
                                                return 1;
                                            }
                                            Files.writeString(resolvedOutputFile, diagram);
                                            spec.commandLine().getOut().println("✓ Diagram written to: " + resolvedOutputFile);
                                        } else {
                                            spec.commandLine().getOut().println("\n" + diagram);
                                        }
                                        return 0;
                                    })
                                    .fold(
                                            e -> {
                                                spec.commandLine()
                                                        .getErr()
                                                        .println("Error: Failed to read/write file: " + e.getMessage());
                                                return 1;
                                            },
                                            result -> result);
                        });
    }
}
