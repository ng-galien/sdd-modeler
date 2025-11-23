package io.statemodeler.cli.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities for resolving and preparing Path objects for CLI commands.
 * <p>
 * - Resolves relative paths against the current process working directory
 * - Ensures parent folders exist when creating files
 */
public final class PathUtils {

    private PathUtils() {}

    /**
     * Resolve a possibly relative path against the current Java process working directory.
     * Absolute paths are returned unchanged; relative paths are resolved and normalized.
     */
    public static Path resolveFromProcess(Path p) {
        if (p == null) return null;
        if (p.isAbsolute()) return p.normalize();
        // Prefer the shell's PWD if available (Gradle forwards it). Fallback to user.dir.
        String envPwd = System.getenv("PWD");
        Path cwd = envPwd != null && !envPwd.isBlank() ? Paths.get(envPwd).toAbsolutePath() : Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        return cwd.resolve(p).normalize();
    }

    /**
     * Ensure the parent directory of the given file path exists by creating it if necessary.
     */
    public static void ensureParentDirectoryExists(Path file) throws IOException {
        if (file == null) return;
        Path parent = file.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Ensure a directory exists by creating it and any parent folders.
     */
    public static void ensureDirectoryExists(Path dir) throws IOException {
        if (dir == null) return;
        if (!Files.exists(dir)) Files.createDirectories(dir);
    }
}
