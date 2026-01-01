package io.statemodeler.maven;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.core.SddModel;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

abstract class AbstractSddMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "sdd.modelFile", defaultValue = "${project.basedir}/src/main/resources/sdd.yaml")
    private Path modelFile;

    @Parameter(property = "sdd.outputDir", defaultValue = "${project.build.directory}/generated/sdd")
    private Path outputDir;

    @Parameter(property = "sdd.ddlOutputDir", defaultValue = "${project.build.directory}/generated/sdd/ddl")
    private Path ddlOutputDir;

    @Parameter(property = "sdd.language", defaultValue = "java")
    private String language;

    @Parameter(property = "sdd.generateController", defaultValue = "true")
    private boolean generateController;

    @Parameter(property = "sdd.generateRepository", defaultValue = "true")
    private boolean generateRepository;

    @Parameter(property = "sdd.generateMcp", defaultValue = "true")
    private boolean generateMcp;

    @Parameter(property = "sdd.liquibase", defaultValue = "false")
    private boolean liquibase;

    protected MavenProject project() {
        return project;
    }

    protected Path modelFile() {
        return modelFile;
    }

    protected Path outputDir() {
        return outputDir;
    }

    protected Path ddlOutputDir() {
        return ddlOutputDir;
    }

    protected String language() {
        return language;
    }

    protected boolean generateController() {
        return generateController;
    }

    protected boolean generateRepository() {
        return generateRepository;
    }

    protected boolean generateMcp() {
        return generateMcp;
    }

    protected boolean liquibase() {
        return liquibase;
    }

    protected SddModel loadValidatedModel() throws MojoExecutionException {
        var modelPath = modelFile();
        if (!Files.exists(modelPath)) {
            throw new MojoExecutionException("Model file does not exist: " + modelPath.toAbsolutePath());
        }

        var loader = ModelLoader.forFile(modelPath);
        var model = loader
                .loadFromFile(modelPath)
                .getOrElseThrow(t -> new MojoExecutionException("Failed to parse model file: " + t.getMessage(), t));

        var validation = ModelValidators.getInstance().validate(model);
        if (validation.isInvalid()) {
            var message = validation.getError().stream()
                    .map(err -> " - " + err.message())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new MojoExecutionException("Model validation failed:" + System.lineSeparator() + message);
        }

        return model;
    }
}
