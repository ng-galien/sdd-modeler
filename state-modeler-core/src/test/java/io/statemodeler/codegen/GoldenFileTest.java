package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Utility for golden file (snapshot) testing of code generation.
 * Compares generated files against expected files stored in
 * src/test/resources/expected/.
 *
 * Note: Generated code is already formatted by PebbleCodeGenerator, so no
 * additional
 * formatting is needed here.
 */
public class GoldenFileTest {

    private static final boolean UPDATE_MODE = Boolean.getBoolean("updateExpected");

    /**
     * Asserts that generated files match expected golden files.
     *
     * @param scenario  The test scenario name (subdirectory in expected/)
     * @param generated Map of filename to content
     */
    public static void assertGeneratedMatchesExpected(String scenario, Map<String, String> generated) {
        if (UPDATE_MODE) {
            updateExpectedFiles(scenario, generated);
            System.out.println("Updated expected files for scenario: " + scenario);
            return;
        }

        Path expectedDir = getExpectedDir(scenario);
        assertTrue(
                Files.isDirectory(expectedDir),
                "Expected directory not found: " + expectedDir + ". Run with -DupdateExpected=true to create it.");

        for (Map.Entry<String, String> entry : generated.entrySet()) {
            String filename = entry.getKey();
            String actualContent = entry.getValue();
            Path expectedFile = expectedDir.resolve(filename);

            assertTrue(
                    Files.exists(expectedFile),
                    "Expected file not found: " + expectedFile + ". Run with -DupdateExpected=true to create it.");

            try {
                String expectedContent = Files.readString(expectedFile, StandardCharsets.UTF_8);

                assertEquals(
                        expectedContent,
                        actualContent,
                        "Generated file doesn't match expected: " + filename + "\n" + "Expected file: " + expectedFile
                                + "\n" + "To update expected files, run with -DupdateExpected=true");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read expected file: " + expectedFile, e);
            }
        }
    }

    /**
     * Updates expected files with generated content.
     * Use -DupdateExpected=true to enable this mode.
     *
     * @param scenario  The test scenario name
     * @param generated Map of filename to content
     */
    public static void updateExpectedFiles(String scenario, Map<String, String> generated) {
        Path expectedDir = getExpectedDir(scenario);

        try {
            // Clear existing directory
            if (Files.exists(expectedDir)) {
                Files.walk(expectedDir)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete: " + path, e);
                            }
                        });
            }

            // Write new files (already formatted by PebbleCodeGenerator)
            for (Map.Entry<String, String> entry : generated.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();

                Path targetFile = expectedDir.resolve(filename);
                Files.createDirectories(targetFile.getParent());
                Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to update expected files for scenario: " + scenario, e);
        }
    }

    private static Path getExpectedDir(String scenario) {
        return Paths.get("src/test/resources/expected", scenario);
    }
}
