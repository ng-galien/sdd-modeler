package io.statemodeler.maven;

import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.sql.LiquibaseYamlRenderer;
import io.statemodeler.validation.ModelValidators;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven goal that loads an SDD model and generates DDL into the configured output directory.
 */
@Mojo(name = "generate-sdd-ddl", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateSddDdlMojo extends AbstractMojo {

    @Parameter(property = "sdd.modelFile", defaultValue = "${basedir}/src/main/resources/sdd.yaml", required = true)
    private File modelFile;

    @Parameter(property = "sdd.ddlOutputDir", defaultValue = "${project.build.directory}/generated-sources/sdd/ddl")
    private File outputDir;

    @Parameter(property = "sdd.liquibase", defaultValue = "false")
    private boolean liquibase;

    @Override
    public void execute() throws MojoExecutionException {
        if (outputDir == null) {
            outputDir = new File(projectBuildDir(), "generated-sources/sdd/ddl");
        }

        Path modelPath = modelFile.toPath();
        if (!Files.exists(modelPath)) {
            throw new MojoExecutionException("Model file does not exist: " + modelPath);
        }

        var loader = ModelLoader.forFile(modelPath);
        var model = loader.loadFromFile(modelPath)
                .getOrElseThrow(t -> new MojoExecutionException("Failed to parse model file: " + t.getMessage(), t));

        var validation = ModelValidators.getInstance().validate(model);
        if (validation.isInvalid()) {
            var message = validation.getError().stream()
                    .map(err -> " - " + err.message())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new MojoExecutionException("Model validation failed:" + System.lineSeparator() + message);
        }

        var generator = DdlGenerators.forDialect(model.database().dialect());
        var ddl = generator.generateFormattedDdl(model);

        Path baseDir = outputDir.toPath();
        if (liquibase) {
            writeFile(baseDir.resolve("changelog.yaml"), LiquibaseYamlRenderer.render(ddl));
            getLog().info("Generated DDL (Liquibase YAML) to " + baseDir.toAbsolutePath());
        } else {
            writeFile(baseDir.resolve("schema.sql"), ddl);
            getLog().info("Generated DDL (SQL) to " + baseDir.toAbsolutePath());
        }
    }

    private void writeFile(Path target, String content) throws MojoExecutionException {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write generated file " + target + ": " + e.getMessage(), e);
        }
    }

    private String projectBuildDir() {
        // Maven injects ${project.build.directory} into system properties
        String buildDir = System.getProperty("project.build.directory");
        return buildDir != null ? buildDir : "target";
    }
}
