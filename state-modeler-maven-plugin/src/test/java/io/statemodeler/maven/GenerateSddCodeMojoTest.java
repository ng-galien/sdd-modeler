package io.statemodeler.maven;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateSddCodeMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void failsWhenModelMissing() {
        GenerateSddCodeMojo mojo = new GenerateSddCodeMojo();
        setField(mojo, "modelFile", tempDir.resolve("missing.yaml").toFile());
        setField(mojo, "outputDir", tempDir.resolve("out").toFile());
        setField(mojo, "project", new MavenProject());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Model file does not exist");
    }

    @Test
    void coherentFlagsRequired() throws Exception {
        Path model = tempDir.resolve("model.yaml");
        Files.copy(Path.of("..", "sample", "src", "main", "resources", "sdd.yaml"), model);

        GenerateSddCodeMojo mojo = new GenerateSddCodeMojo();
        setField(mojo, "modelFile", model.toFile());
        setField(mojo, "outputDir", tempDir.resolve("out").toFile());
        setField(mojo, "language", "java");
        setField(mojo, "generateController", true);
        setField(mojo, "generateRepository", false);
        setField(mojo, "generateMcp", false);
        setField(mojo, "project", new MavenProject());

        assertThatThrownBy(mojo::execute).isInstanceOf(Exception.class).hasMessageContaining("Invalid configuration");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
