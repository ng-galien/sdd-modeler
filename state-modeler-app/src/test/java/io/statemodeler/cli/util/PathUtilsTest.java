package io.statemodeler.cli.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveFromProcessRelativePathShouldResolveAgainstCwd() {
        var relative = Path.of("README.md");
        var resolved = PathUtils.resolveFromProcess(relative);
        assertTrue(resolved.endsWith("README.md"));
        assertTrue(resolved.toAbsolutePath().startsWith(Path.of(System.getProperty("user.dir")).toAbsolutePath()));
    }

    @Test
    void ensureParentAndDirectoryCreationWorks(@TempDir Path tmp) throws Exception {
        var dir = tmp.resolve("a/b/c");
        assertFalse(Files.exists(dir));
        PathUtils.ensureDirectoryExists(dir);
        assertTrue(Files.exists(dir));
        var file = dir.resolve("file.txt");
        Files.deleteIfExists(file);
        PathUtils.ensureParentDirectoryExists(file);
        assertTrue(Files.exists(dir));
    }
}
