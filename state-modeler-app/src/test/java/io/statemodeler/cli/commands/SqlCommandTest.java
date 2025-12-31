package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class SqlCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateSqlToStdout() throws Exception {
        // Given
        var validYaml = """
                version: "0.1.0"
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    var exitCode = cmd.execute(modelFile.toString());

                    // Then
                    assertEquals(0, exitCode);
                    var output = result.out();
                    assertTrue(output.contains("-- Generated DDL for test-model"));
                    assertTrue(output.contains("CREATE TABLE public.orders"));
                    assertTrue(output.contains("CREATE TABLE public_states.order_pending"));
                },
                modelFile.toString());
    }

    @Test
    void shouldGenerateSqlToFile() throws Exception {
        // Given
        var validYaml = """
                version: "0.1.0"
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    var exitCode = cmd.execute(modelFile.toString(), "-o", outputFile.toString());

                    // Then
                    assertEquals(0, exitCode);

                    try {
                        var generatedSql = Files.readString(outputFile);
                        assertTrue(generatedSql.contains("CREATE TABLE"));
                        assertTrue(generatedSql.contains("orders"));
                        assertTrue(generatedSql.contains("order_pending"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                modelFile.toString(),
                "-o",
                outputFile.toString());
    }

    @Test
    void shouldRejectInvalidDialect() throws Exception {
        // Given
        var validYaml = """
                version: "0.1.0"
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    var exitCode = cmd.execute(modelFile.toString(), "--dialect", "mysql");

                    // Then
                    assertEquals(1, exitCode);
                    assertTrue(result.err().contains("Error: Unsupported SQL dialect 'mysql'"));
                },
                modelFile.toString(),
                "--dialect",
                "mysql");
    }

    @Test
    void shouldRejectInvalidModel() throws Exception {
        // Given - model with validation errors
        var invalidYaml = """
                version: "0.1.0"
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
                        from: "nonexistent"
                        table: order_paid
                """;

        var modelFile = tempDir.resolve("invalid-model.yaml");
        Files.writeString(modelFile, invalidYaml);

        var command = new SqlCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    var exitCode = cmd.execute(modelFile.toString());

                    // Then
                    assertEquals(1, exitCode);
                    assertTrue(result.err().contains("✗ Model validation failed"));
                },
                modelFile.toString());
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new SqlCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    var exitCode = cmd.execute(nonExistentFile.toString());

                    // Then
                    assertEquals(1, exitCode);
                    assertTrue(result.err().contains("Error: Model file does not exist"));
                },
                nonExistentFile.toString());
    }
}
