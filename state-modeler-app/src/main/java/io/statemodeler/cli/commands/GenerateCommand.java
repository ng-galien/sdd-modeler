package io.statemodeler.cli.commands;

import io.statemodeler.codegen.CodeGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import io.statemodeler.cli.util.PathUtils;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "generate", description = "Generate source code from an SDD model file")
public class GenerateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(GenerateCommand.class);

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)")
    private Path modelFile;

    @Option(
            names = {"--language"},
            description = "Target language for generation (default: java)",
            defaultValue = "java")
    private String language;

    @Option(
            names = {"-o", "--outdir"},
            description = "Output directory for generated sources (default: stdout)")
    private Path outDir;

    @Override
    public Integer call() {
        logger.info("Generating code for model file: {}", modelFile);
        logger.info("Language: {}", language);

        if (!CodeGenerators.isSupported(language)) {
            spec.commandLine().getErr().println("Error: Unsupported generation language '" + language + "'");
            spec.commandLine()
                    .getErr()
                    .println("Supported languages: " + String.join(", ", CodeGenerators.getSupportedLanguages()));
            return 1;
        }

        var resolvedModelFile = PathUtils.resolveFromProcess(modelFile);
        var resolvedOutDir = outDir == null ? null : PathUtils.resolveFromProcess(outDir);

        if (!Files.exists(resolvedModelFile)) {
            spec.commandLine().getErr().println("Error: Model file does not exist: " + resolvedModelFile);
            return 1;
        }

        return io.vavr.control.Try.of(() -> modelFile)
                .map(f -> ModelLoader.forFile(f))
                .flatMap(loader -> loader.loadFromFile(resolvedModelFile))
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

                            var generator = CodeGenerators.forLanguage(language);
                            Map<String, String> generated = generator.generate(model);

                            if (resolvedOutDir != null) {
                                try {
                                    PathUtils.ensureDirectoryExists(resolvedOutDir);
                                } catch (IOException e) {
                                    spec.commandLine()
                                            .getErr()
                                            .println("Error: Could not create output directory: " + e.getMessage());
                                    return 1;
                                }
                                for (var entry : generated.entrySet()) {
                                        var path = resolvedOutDir.resolve(entry.getKey());
                                    try {
                                        Files.createDirectories(path.getParent());
                                        Files.writeString(path, entry.getValue());
                                    } catch (IOException e) {
                                        spec.commandLine()
                                                .getErr()
                                                .println("Error writing generated file: " + e.getMessage());
                                        return 1;
                                    }
                                }
                                spec.commandLine()
                                        .getOut()
                                        .println("✓ Generated " + generated.size() + " files to " + outDir);
                            } else {
                                // Print to stdout
                                for (var entry : generated.entrySet()) {
                                    spec.commandLine().getOut().println("--- " + entry.getKey() + " ---");
                                    spec.commandLine().getOut().println(entry.getValue());
                                }
                            }

                            return 0;
                        });
    }
}
