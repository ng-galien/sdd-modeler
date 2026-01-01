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
    private final DirectoryProperty ddlOutputDir;
    private final Property<String> language;
    private final Property<Boolean> addToSourceSet;
    private final Property<Boolean> generateController;
    private final Property<Boolean> generateRepository;
    private final Property<Boolean> generateMcp;
    private final Property<Boolean> liquibase;

    public SddCodegenExtension(Project project) {
        var objects = project.getObjects();
        var layout = project.getLayout();
        this.modelFile =
                objects.fileProperty().convention(layout.getProjectDirectory().file("src/main/resources/sdd.yaml"));
        this.outputDir = objects.directoryProperty();
        this.outputDir.convention(layout.getBuildDirectory().dir("generated/sdd"));
        this.ddlOutputDir = objects.directoryProperty();
        this.ddlOutputDir.convention(layout.getBuildDirectory().dir("generated/sdd/ddl"));
        this.language = objects.property(String.class).convention("java");
        this.addToSourceSet = objects.property(Boolean.class).convention(true);
        this.generateController = objects.property(Boolean.class).convention(true);
        this.generateRepository = objects.property(Boolean.class).convention(true);
        this.generateMcp = objects.property(Boolean.class).convention(true);
        this.liquibase = objects.property(Boolean.class).convention(false);
    }

    public RegularFileProperty getModelFile() {
        return modelFile;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public DirectoryProperty getDdlOutputDir() {
        return ddlOutputDir;
    }

    public Property<String> getLanguage() {
        return language;
    }

    public Property<Boolean> getAddToSourceSet() {
        return addToSourceSet;
    }

    public Property<Boolean> getGenerateController() {
        return generateController;
    }

    public Property<Boolean> getGenerateRepository() {
        return generateRepository;
    }

    public Property<Boolean> getGenerateMcp() {
        return generateMcp;
    }

    public Property<Boolean> getLiquibase() {
        return liquibase;
    }
}
