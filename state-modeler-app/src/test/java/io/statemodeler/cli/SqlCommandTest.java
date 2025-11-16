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

class SqlCommandTest {

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
    void shouldGenerateSqlToStdout() throws Exception {
        // Given
        var validYaml = """
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

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var command = new SqlCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertEquals(0, exitCode);
        var output = outContent.toString();
        assertTrue(output.contains("✓ Model parsed successfully"));
        assertTrue(output.contains("-- Generated DDL for test-model"));
        assertTrue(output.contains("CREATE TABLE public.orders"));
        assertTrue(output.contains("CREATE TABLE public_states.order_pending"));
    }

    @Test
    void shouldGenerateSqlToFile() throws Exception {
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

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var outputFile = tempDir.resolve("output.sql");

        var command = new SqlCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "-o", outputFile.toString());

        // Then
        assertEquals(0, exitCode);
        assertTrue(outContent.toString().contains("✓ DDL written to"));

        var generatedSql = Files.readString(outputFile);
        assertTrue(generatedSql.contains("CREATE TABLE"));
        assertTrue(generatedSql.contains("orders"));
        assertTrue(generatedSql.contains("order_pending"));
    }

    @Test
    void shouldRejectInvalidDialect() throws Exception {
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

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var command = new SqlCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "--dialect", "mysql");

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Error: Unsupported SQL dialect 'mysql'"));
    }

    @Test
    void shouldRejectInvalidModel() throws Exception {
        // Given - model with validation errors
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
                      paid:
                        from: [nonexistent]
                        table: order_paid
                """;

        var modelFile = tempDir.resolve("invalid-model.yaml");
        Files.writeString(modelFile, invalidYaml);

        var command = new SqlCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("✗ Model validation failed"));
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new SqlCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(nonExistentFile.toString());

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Error: Model file does not exist"));
    }
}
