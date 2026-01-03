package io.statemodeler.maven;

import io.statemodeler.codegen.CodeGenerators;
import io.statemodeler.dsl.ModelLoader;
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
import org.apache.maven.project.MavenProject;

/**
 * Maven goal that loads an SDD model and generates sources into the configured output directory.
 */
@Mojo(name = "generate-sdd-code", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateSddCodeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "sdd.modelFile", defaultValue = "${basedir}/src/main/resources/sdd.yaml", required = true)
    private File modelFile;

    @Parameter(property = "sdd.outputDir", defaultValue = "${project.build.directory}/generated-sources/sdd")
    private File outputDir;

    @Parameter(property = "sdd.language", defaultValue = "java")
    private String language;

    @Parameter(property = "sdd.generateController", defaultValue = "true")
    private boolean generateController;

    @Parameter(property = "sdd.generateRepository", defaultValue = "true")
    private boolean generateRepository;

    @Parameter(property = "sdd.generateMcp", defaultValue = "true")
    private boolean generateMcp;

    @Parameter(property = "sdd.addToSource", defaultValue = "true")
    private boolean addToSource;

    @Parameter(property = "sdd.disableFormatter", defaultValue = "false")
    private boolean disableFormatter;

    private static final String INCOHERENT_MSG =
            "Invalid configuration: repositories/services must be generated when REST or MCP generation is enabled";

    @Override
    public void execute() throws MojoExecutionException {
        if (outputDir == null) {
            String buildDir = project != null
                    ? project.getBuild().getDirectory()
                    : System.getProperty("project.build.directory", "target");
            outputDir = new File(buildDir, "generated-sources/sdd");
        }

        Path modelPath = modelFile.toPath();
        if (!Files.exists(modelPath)) {
            throw new MojoExecutionException("Model file does not exist: " + modelPath);
        }

        var resolvedLanguage = language == null ? "java" : language;
        if (disableFormatter) {
            System.setProperty("sdd.disableFormatter", "true");
        }
        if (!CodeGenerators.isSupported(resolvedLanguage)) {
            throw new MojoExecutionException(
                    "Unsupported generation language '" + resolvedLanguage + "'. Supported: java");
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

        boolean generateControllerFlag = generateController;
        boolean generateRepositoryFlag = generateRepository;
        boolean generateMcpFlag = generateMcp;
        assertCoherent(generateControllerFlag, generateRepositoryFlag, generateMcpFlag);

        var patchedModel = model.withGeneratorOptions(generateControllerFlag, generateRepositoryFlag, generateMcpFlag);
        var generator = CodeGenerators.forLanguage(resolvedLanguage);
        var generated = generator.generate(patchedModel);

        Path baseDir = outputDir.toPath();
        for (var entry : generated.entrySet()) {
            writeFile(baseDir.resolve(entry.getKey()), entry.getValue());
        }
        getLog().info("Generated " + generated.size() + " files to " + baseDir.toAbsolutePath());

        if (addToSource && project != null) {
            project.addCompileSourceRoot(baseDir.toAbsolutePath().toString());
            getLog().info("Added generated sources to compile classpath: " + baseDir.toAbsolutePath());
        }
    }

    private void assertCoherent(boolean generateController, boolean generateRepository, boolean generateMcp)
            throws MojoExecutionException {
        if ((generateController || generateMcp) && !generateRepository) {
            throw new MojoExecutionException(INCOHERENT_MSG);
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
}
