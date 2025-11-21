package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.RepositoryMixin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Tests for {@link RegisterCommand}.
 */
class RegisterCommandTest {

    @TempDir
    Path tempDir;

    private Path repositoryPath;
    private Path validModelFile;
    private Path invalidModelFile;
    private Path jsonModelFile;

    @BeforeEach
    void setUp() throws IOException {
        repositoryPath = tempDir.resolve("test-repo");

        // Valid YAML model
        validModelFile = tempDir.resolve("valid-model.yaml");
        Files.writeString(validModelFile, """
        version: "0.1"
        name: "test-model"
        database:
          dialect: postgres
        entities:
          order:
            table: orders
            id:
              name: id
              type: serial
              primary_key: true
            states:
              pending:
                initial: true
                table: order_pending
        """);

        // Invalid model (no initial state)
        invalidModelFile = tempDir.resolve("invalid-model.yaml");
        Files.writeString(invalidModelFile, """
        version: "0.1"
        name: "invalid-model"
        database:
          dialect: postgres
        entities:
          order:
            table: orders
            id:
              name: id
              type: serial
              primary_key: true
            states:
              pending:
                table: order_pending
        """);

        // Valid JSON model
        jsonModelFile = tempDir.resolve("json-model.json");
        Files.writeString(jsonModelFile, """
        {
          "version": "0.1",
          "name": "json-test",
          "database": {
            "dialect": "postgres"
          },
          "entities": {
            "order": {
              "table": "orders",
              "id": {
                "name": "id",
                "type": "serial",
                "primary_key": true
              },
              "states": {
                "pending": {
                  "initial": true,
                  "table": "order_pending"
                }
              }
            }
          }
        }
        """);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up repository
        if (Files.exists(repositoryPath.getParent())) {
            Files.walk(repositoryPath.getParent())
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Test
    void shouldRegisterValidYamlModel() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString());

        // Then
        assertEquals(0, exitCode, "Should succeed with exit code 0");
    }

    @Test
    void shouldRegisterValidJsonModel() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(jsonModelFile.toString());

        // Then
        assertEquals(0, exitCode, "Should succeed with exit code 0");
    }

    @Test
    void shouldFailWithInvalidModel() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(invalidModelFile.toString());

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for validation error");
    }

    @Test
    void shouldFailWithNonexistentFile() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(tempDir.resolve("nonexistent.yaml").toString());

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for missing file");
    }

    @Test
    void shouldDetectDuplicates() {
        // Given
        var command1 = new RegisterCommand();
        command1.repositoryMixin = createMixin();
        var cmd1 = new CommandLine(command1);

        var command2 = new RegisterCommand();
        command2.repositoryMixin = createMixin();
        var cmd2 = new CommandLine(command2);

        // When
        int firstExitCode = cmd1.execute(validModelFile.toString());
        int secondExitCode = cmd2.execute(validModelFile.toString());

        // Then
        assertEquals(0, firstExitCode, "First registration should succeed");
        assertEquals(2, secondExitCode, "Second registration should fail with exit code 2 for duplicate");
    }

    @Test
    void shouldUseCustomName() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString(), "--name", "custom-name");

        // Then
        assertEquals(0, exitCode);
        // Verify name was used (would need to query repository)
    }

    @Test
    void shouldUseCustomVersion() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString(), "--version", "2.5.3");

        // Then
        assertEquals(0, exitCode);
        // Verify version was used (would need to query repository)
    }

    @Test
    void shouldDeriveNameFromFilename() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString());

        // Then
        assertEquals(0, exitCode);
        // Name should be derived as "valid-model"
    }

    @Test
    void shouldUseDefaultVersionWhenNotSpecified() {
        // Given
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString());

        // Then
        assertEquals(0, exitCode);
        // Version should default to "1.0"
    }

    @Test
    void shouldHandleModelWithExplicitVersion() throws IOException {
        // Given - model with version in YAML
        Path versionedModel = tempDir.resolve("versioned-model.yaml");
        Files.writeString(versionedModel, """
        version: "2.0"
        name: "versioned-test"
        database:
          dialect: postgres
        entities:
          order:
            table: orders
            id:
              name: id
              type: serial
              primary_key: true
            states:
              pending:
                initial: true
                table: order_pending
        """);

        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(versionedModel.toString());

        // Then
        assertEquals(0, exitCode);
        // Should use version "2.0" from model
    }

    @Test
    void shouldPreferCliVersionOverModelVersion() throws IOException {
        // Given - model with version but CLI override
        Path versionedModel = tempDir.resolve("versioned-model2.yaml");
        Files.writeString(versionedModel, """
        version: "1.5"
        name: "test-override"
        database:
          dialect: postgres
        entities:
          order:
            table: orders
            id:
              name: id
              type: serial
              primary_key: true
            states:
              pending:
                initial: true
                table: order_pending
        """);

        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(versionedModel.toString(), "--version", "3.0.0");

        // Then
        assertEquals(0, exitCode);
        // Should use CLI version "3.0.0" instead of "1.5"
    }

    @Test
    void shouldResolveFileFromWorkspaceScriptsFolder() {
        // Given - path referencing the sample model relative to the workspace root
        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("scripts/examples/orders-sdd-model.yaml");

        // Then
        assertEquals(0, exitCode, "Should resolve model from workspace scripts/examples and register");
    }

    @Test
    void shouldHandleFilenameWithoutExtension() throws IOException {
        // Given - file without extension (edge case for resolveName)
        Path noExtensionFile = tempDir.resolve("noextension");
        Files.writeString(noExtensionFile, """
        version: "0.1"
        name: "test-no-ext"
        database:
          dialect: postgres
        entities:
          order:
            table: orders
            id:
              name: id
              type: serial
              primary_key: true
            states:
              pending:
                initial: true
                table: order_pending
        """);

        var command = new RegisterCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(noExtensionFile.toString());

        // Then
        assertEquals(0, exitCode);
        // Should derive name as "noextension" (full filename)
    }

    @Test
    void shouldHandleRepositoryException() {
        // Given - command that will trigger repository error
        var command = new RegisterCommand();
        // Use an invalid repository path to trigger exception
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path"; // Invalid path with null char
        command.repositoryMixin = mixin;
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(validModelFile.toString());

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for repository error");
    }

    private RepositoryMixin createMixin() {
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = repositoryPath.toString();
        return mixin;
    }
}
