package io.statemodeler.gradle;

import io.statemodeler.codegen.CodeGenerators;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.validation.ModelValidators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Task that loads an SDD model and generates sources into the configured output directory.
 */
public abstract class GenerateSddCodeTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getModelFile();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getLanguage();

    @TaskAction
    public void generate() {
        var modelPath = getModelFile().get().getAsFile().toPath();
        if (!Files.exists(modelPath)) {
            throw new GradleException("Model file does not exist: " + modelPath);
        }

        var language = getLanguage().getOrElse("java");
        if (!CodeGenerators.isSupported(language)) {
            throw new GradleException("Unsupported generation language '" + language + "'. Supported: java");
        }

        var loader = ModelLoader.forFile(modelPath);
        var model = loader.loadFromFile(modelPath)
                .getOrElseThrow(t -> new GradleException("Failed to parse model file: " + t.getMessage(), t));

        var validation = ModelValidators.getInstance().validate(model);
        if (validation.isInvalid()) {
            var message = validation.getError().stream()
                    .map(err -> " - " + err.message())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new GradleException("Model validation failed:" + System.lineSeparator() + message);
        }

        var generator = CodeGenerators.forLanguage(language);
        var generated = generator.generate(model);
        var baseDir = getOutputDir().get().getAsFile().toPath();

        generated.forEach((relativePath, content) -> writeFile(baseDir.resolve(relativePath), content));

        getLogger().lifecycle("Generated {} files to {}", generated.size(), baseDir.toAbsolutePath());
    }

    private void writeFile(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new GradleException("Failed to write generated file " + target + ": " + e.getMessage(), e);
        }
    }
}
