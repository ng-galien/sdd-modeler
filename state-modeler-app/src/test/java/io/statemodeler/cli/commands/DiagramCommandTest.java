package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
// no BeforeEach/AfterEach stream manipulation - using PicocliTestHelper instead
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class DiagramCommandTest {

    @TempDir
    Path tempDir;

    // Use PicocliTestHelper.capture(cmd) per-test to capture both output and logging

    @Test
    void shouldGenerateDiagramToStdout() throws Exception {
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
                        attributes:
                          reason:
                            type: text
                            nullable: false
                      paid:
                        from: "pending"
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    // When
                    // Then
                    assertEquals(0, result.exitCode());
                    var output = result.out() + result.err();
                    assertTrue(output.contains("✓ Model parsed successfully"));
                    assertTrue(output.contains("✓ Model validation passed"));
                    assertTrue(output.contains("stateDiagram-v2"));
                    assertTrue(output.contains("[*] --> pending"));
                    assertTrue(output.contains("pending --> paid"));
                },
                modelFile.toString());
    }

    @Test
    void shouldGenerateDiagramToFile() throws Exception {
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    assertTrue(Files.exists(outputFile));
                    try {
                        var diagramContent = Files.readString(outputFile);
                        assertTrue(diagramContent.contains("stateDiagram-v2"));
                        assertTrue(diagramContent.contains("[*] --> pending"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertTrue((result.out() + result.err()).contains("✓ Diagram written to"));
                },
                modelFile.toString(),
                "-o",
                outputFile.toString());
    }

    @Test
    void shouldGenerateDiagramForSpecificEntity() throws Exception {
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
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    var output = result.out() + result.err();
                    assertTrue(output.contains("stateDiagram-v2"));
                    assertTrue(output.contains("order State Diagram"));
                    assertFalse(output.contains("shipment"));
                },
                modelFile.toString(),
                "-e",
                "order");
    }

    @Test
    void shouldRejectNonExistentFile() {
        // Given
        var nonExistentFile = tempDir.resolve("does-not-exist.yaml");

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    assertTrue(result.err().contains("Error: Model file does not exist"));
                },
                nonExistentFile.toString());
    }

    @Test
    void shouldRejectInvalidFormat() throws Exception {
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

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    assertTrue(result.err().contains("Error: Unsupported diagram format"));
                    assertTrue(result.err().contains("Supported formats: mermaid"));
                },
                modelFile.toString(),
                "-f",
                "plantuml");
    }

    @Test
    void shouldRejectNonExistentEntity() throws Exception {
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

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    assertTrue(result.err().contains("Error: Entity not found"));
                },
                modelFile.toString(),
                "-e",
                "shipment");
    }

    @Test
    void shouldRejectInvalidModel() throws Exception {
        // Given - model with no initial state
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
                """;

        var modelFile = tempDir.resolve("invalid-model.yaml");
        Files.writeString(modelFile, invalidYaml);

        var command = new DiagramCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(1, result.exitCode());
                    assertTrue(result.err().contains("✗ Model validation failed"));
                },
                modelFile.toString());
    }
}
