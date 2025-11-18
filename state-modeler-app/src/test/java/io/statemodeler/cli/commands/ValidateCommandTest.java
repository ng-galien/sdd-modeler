package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ValidateCommandTest {

    @TempDir
    Path tempDir;

    // Use PicocliTestHelper.capture(cmd) per-test to capture both command output and logging

    @Test
    void shouldValidateValidYamlModel() throws Exception {
        // Given
        var validYaml = """
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
                        attributes:
                          reason:
                            type: text
                            nullable: false
                """;

        var modelFile = tempDir.resolve("valid-model.yaml");
        Files.writeString(modelFile, validYaml);

        var command = new ValidateCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out() + result.err();
                    assertTrue(output.contains("✓ Model parsed successfully"));
                    assertTrue(output.contains("✓ Model validation passed"));
                },
                modelFile.toString());
    }

    @Test
    void shouldRejectInvalidYamlModel() throws Exception {
        // Given - model with no initial state
        var invalidYaml = """
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
                        initial: false
                        table: order_pending
                """;

        var modelFile = tempDir.resolve("invalid-model.yaml");
        Files.writeString(modelFile, invalidYaml);

        var command = new ValidateCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    String err = result.err();
                    assertTrue(err.contains("✗ Model validation failed"));
                    assertTrue(err.contains("Entity must have exactly one initial state"));
                },
                modelFile.toString());
    }

    @Test
    void shouldRejectMalformedYaml() throws Exception {
        // Given
        var malformedYaml = """
                version: "0.1"
                name: "test
                  bad indentation
                """;

        var modelFile = tempDir.resolve("malformed.yaml");
        Files.writeString(modelFile, malformedYaml);

        var command = new ValidateCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    String err = result.err();
                    assertTrue(err.contains("✗ Failed to parse model file"));
                },
                modelFile.toString());
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new ValidateCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    String err = result.err();
                    assertTrue(err.contains("Error: Model file does not exist"));
                },
                nonExistentFile.toString());
    }
}
