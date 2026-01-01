package io.statemodeler.maven;

import io.statemodeler.codegen.CodeGenerators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateMojo extends AbstractSddMojo {

    @Override
    public void execute() throws MojoExecutionException {
        var model = loadValidatedModel();

        var language = language();
        if (!CodeGenerators.isSupported(language)) {
            throw new MojoExecutionException("Unsupported generation language '" + language + "'. Supported: java");
        }

        boolean generateController = generateController();
        boolean generateRepository = generateRepository();
        boolean generateMcp = generateMcp();
        assertCoherent(generateController, generateRepository, generateMcp);

        var patchedModel = model.withGeneratorOptions(generateController, generateRepository, generateMcp);
        var generator = CodeGenerators.forLanguage(language);
        var generated = generator.generate(patchedModel);

        var baseDir = outputDir();
        generated.forEach((relativePath, content) -> {
            try {
                writeFile(baseDir.resolve(relativePath), content);
            } catch (MojoExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        // Make generated sources available to Maven compilation.
        project().addCompileSourceRoot(baseDir.toString());

        getLog().info("Generated " + generated.size() + " files to " + baseDir.toAbsolutePath());
    }

    private void assertCoherent(boolean generateController, boolean generateRepository, boolean generateMcp)
            throws MojoExecutionException {
        if ((generateController || generateMcp) && !generateRepository) {
            throw new MojoExecutionException(
                    "Invalid configuration: repositories/services must be generated when REST or MCP generation is enabled");
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
