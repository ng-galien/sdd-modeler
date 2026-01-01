package io.statemodeler.maven;

import io.statemodeler.sql.DdlGenerators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-ddl", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateDdlMojo extends AbstractSddMojo {

    @Override
    public void execute() throws MojoExecutionException {
        var model = loadValidatedModel();
        var generator = DdlGenerators.forDialect(model.database().dialect());
        var ddl = generator.generateFormattedDdl(model);

        var baseDir = ddlOutputDir();
        if (liquibase()) {
            writeFile(baseDir.resolve("changelog.yaml"), LiquibaseYamlRenderer.render(ddl));
            getLog().info("Generated Liquibase changelog to " + baseDir.toAbsolutePath());
        } else {
            writeFile(baseDir.resolve("schema.sql"), ddl);
            getLog().info("Generated SQL DDL to " + baseDir.toAbsolutePath());
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
