package io.statemodeler.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Extension to configure SDD code generation.
 */
public class SddCodegenExtension {

    private final RegularFileProperty modelFile;
    private final DirectoryProperty outputDir;
    private final Property<String> language;
    private final Property<Boolean> addToSourceSet;

    public SddCodegenExtension(Project project) {
        var objects = project.getObjects();
        var layout = project.getLayout();
        this.modelFile =
                objects.fileProperty().convention(layout.getProjectDirectory().file("src/main/resources/sdd.yaml"));
        this.outputDir = objects.directoryProperty();
        this.outputDir.convention(layout.getBuildDirectory().dir("generated/sdd"));
        this.language = objects.property(String.class).convention("java");
        this.addToSourceSet = objects.property(Boolean.class).convention(true);
    }

    public RegularFileProperty getModelFile() {
        return modelFile;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<String> getLanguage() {
        return language;
    }

    public Property<Boolean> getAddToSourceSet() {
        return addToSourceSet;
    }
}
