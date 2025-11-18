package io.statemodeler.cli.commands;

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

class DiagramCommandTest {

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
    void shouldGenerateDiagramToStdout() throws Exception {
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
                      paid:
                        from: [pending]
                        table: order_paid
                        attributes:
                          payment_method:
                            type: text
                            nullable: false
                """;

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertEquals(0, exitCode);
        var output = outContent.toString();
        assertTrue(output.contains("✓ Model parsed successfully"));
        assertTrue(output.contains("✓ Model validation passed"));
        assertTrue(output.contains("stateDiagram-v2"));
        assertTrue(output.contains("[*] --> pending"));
        assertTrue(output.contains("pending --> paid"));
    }

    @Test
    void shouldGenerateDiagramToFile() throws Exception {
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

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var outputFile = tempDir.resolve("output.mmd");
        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "-o", outputFile.toString());

        // Then
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outputFile));
        var diagramContent = Files.readString(outputFile);
        assertTrue(diagramContent.contains("stateDiagram-v2"));
        assertTrue(diagramContent.contains("[*] --> pending"));
        assertTrue(outContent.toString().contains("✓ Diagram written to"));
    }

    @Test
    void shouldGenerateDiagramForSpecificEntity() throws Exception {
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
                  shipment:
                    table: shipments
                    id:
                      name: id
                      type: serial
                      primary_key: true
                    states:
                      preparing:
                        initial: true
                        table: shipment_preparing
                """;

        var modelFile = tempDir.resolve("model.yaml");
        Files.writeString(modelFile, validYaml);

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "-e", "order");

        // Then
        assertEquals(0, exitCode);
        var output = outContent.toString();
        assertTrue(output.contains("stateDiagram-v2"));
        assertTrue(output.contains("order State Diagram"));
        assertFalse(output.contains("shipment"));
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(nonExistentFile.toString());

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Error: Model file does not exist"));
    }

    @Test
    void shouldRejectInvalidFormat() throws Exception {
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

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "-f", "plantuml");

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Error: Unsupported diagram format"));
        assertTrue(errContent.toString().contains("Supported formats: mermaid"));
    }

    @Test
    void shouldRejectNonExistentEntity() throws Exception {
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

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString(), "-e", "shipment");

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Error: Entity not found"));
    }

    @Test
    void shouldRejectInvalidModel() throws Exception {
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

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);

        // When
        var exitCode = cmd.execute(modelFile.toString());

        // Then
        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("✗ Model validation failed"));
    }
}
