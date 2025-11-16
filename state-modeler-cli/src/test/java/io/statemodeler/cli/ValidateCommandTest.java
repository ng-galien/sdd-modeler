package io.statemodeler.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ValidateCommandTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

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

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outContent.toString()).contains("✓ Model parsed successfully");
        assertThat(outContent.toString()).contains("✓ Model validation passed");
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

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertThat(exitCode).isEqualTo(1);
        assertThat(errContent.toString()).contains("✗ Model validation failed");
        assertThat(errContent.toString()).contains("Entity must have exactly one initial state");
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

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertThat(exitCode).isEqualTo(1);
        assertThat(errContent.toString()).contains("✗ Failed to parse model file");
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new ValidateCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(nonExistentFile.toString());

        // Then
        assertThat(exitCode).isEqualTo(1);
        assertThat(errContent.toString()).contains("Error: Model file does not exist");
    }
}
