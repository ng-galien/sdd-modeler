package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class DiffCommandTest {

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
    void shouldShowDiffBetweenTwoModels() throws Exception {
        // Given
        var currentYaml = """
                version: "0.1"
                name: "test-model"
                database:
                  dialect: postgres
                  schema: public
                entities:
                  order:
                    table: orders
                    id:
                      name: id
                      type: serial
                      primary_key: true
                    attributes:
                      amount:
                        type: numeric(10,2)
                        nullable: false
                    states:
                      pending:
                        initial: true
                        table: order_pending
                        attributes:
                          reason:
                            type: text
                            nullable: false
                """;

        var futureYaml = """
                version: "0.1"
                name: "test-model"
                database:
                  dialect: postgres
                  schema: public
                entities:
                  order:
                    table: orders
                    id:
                      name: id
                      type: serial
                      primary_key: true
                    attributes:
                      amount:
                        type: numeric(12,2)
                        nullable: false
                    states:
                      pending:
                        initial: true
                        table: order_pending
                        attributes:
                          reason:
                            type: text
                            nullable: false
                """;

        var currentFile = tempDir.resolve("current.yaml");
        var futureFile = tempDir.resolve("future.yaml");
        Files.writeString(currentFile, currentYaml);
        Files.writeString(futureFile, futureYaml);

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(0, exitCode);
        var output = outContent.toString();
        assertTrue(output.contains("✓ Current model parsed"));
        assertTrue(output.contains("✓ Future model parsed"));
        assertTrue(output.contains("DDL Diff:"));
    }

    @Test
    void shouldShowNoDiffForIdenticalModels() throws Exception {
        // Given
        var modelYaml = """
                version: "0.1"
                name: "test-model"
                database:
                  dialect: postgres
                  schema: public
                entities:
                  order:
                    table: orders
                    id:
                      name: id
                      type: serial
                      primary_key: true
                    attributes:
                      amount:
                        type: numeric(10,2)
                        nullable: false
                    states:
                      pending:
                        initial: true
                        table: order_pending
                        attributes:
                          reason:
                            type: text
                            nullable: false
                """;

        var currentFile = tempDir.resolve("current.yaml");
        var futureFile = tempDir.resolve("future.yaml");
        Files.writeString(currentFile, modelYaml);
        Files.writeString(futureFile, modelYaml);

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(0, exitCode);
        var output = outContent.toString();
        assertTrue(output.contains("✓ No differences found - DDL schemas are identical"));
    }

    @Test
    void shouldFailWhenCurrentModelFileDoesNotExist() {
        // Given
        var currentFile = tempDir.resolve("nonexistent.yaml");
        var futureFile = tempDir.resolve("future.yaml");

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(1, exitCode);
    }

    @Test
    void shouldFailWhenFutureModelFileDoesNotExist() throws Exception {
        // Given
        var currentYaml = """
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
                """;

        var currentFile = tempDir.resolve("current.yaml");
        Files.writeString(currentFile, currentYaml);
        var futureFile = tempDir.resolve("nonexistent.yaml");

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(1, exitCode);
    }

    @Test
    void shouldFailWhenCurrentModelIsInvalid() throws Exception {
        // Given
        var invalidYaml = """
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
                """;

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
                """;

        var currentFile = tempDir.resolve("current.yaml");
        var futureFile = tempDir.resolve("future.yaml");
        Files.writeString(currentFile, invalidYaml);
        Files.writeString(futureFile, validYaml);

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(1, exitCode);
    }

    @Test
    void shouldFailWhenFutureModelIsInvalid() throws Exception {
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
                """;

        var invalidYaml = """
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
                """;

        var currentFile = tempDir.resolve("current.yaml");
        var futureFile = tempDir.resolve("future.yaml");
        Files.writeString(currentFile, validYaml);
        Files.writeString(futureFile, invalidYaml);

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString());

        // Then
        assertEquals(1, exitCode);
    }

    @Test
    void shouldFailWhenUnsupportedDialect() throws Exception {
        // Given
        var modelYaml = """
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
                """;

        var currentFile = tempDir.resolve("current.yaml");
        var futureFile = tempDir.resolve("future.yaml");
        Files.writeString(currentFile, modelYaml);
        Files.writeString(futureFile, modelYaml);

        var command = new DiffCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(currentFile.toString(), futureFile.toString(), "--dialect", "mysql");

        // Then
        assertEquals(1, exitCode);
    }
}
