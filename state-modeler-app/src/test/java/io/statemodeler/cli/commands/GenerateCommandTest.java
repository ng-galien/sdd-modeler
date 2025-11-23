package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GenerateCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateJavaFilesToStdout() throws Exception {
        var yaml = """
                version: "0.1.0"
                name: "test-model"
                database:
                  dialect: postgres
                  generator_options:
                    packageName: com.example
                entities:
                  Order:
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
        Files.writeString(modelFile, yaml);

        var command = new GenerateCommand();
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    var out = result.out();
                    assertTrue(out.contains("com/example/Order.java") || out.contains("package com.example;"));
                    assertTrue(out.contains("interface OrderState"));
                },
                modelFile.toString(),
                "--language",
                "java");
    }
}
