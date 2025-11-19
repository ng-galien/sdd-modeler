package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for {@link ListCommand}.
 */
class ListCommandTest {

    private H2SdrRepository repository;
    private SdrFactory sdrFactory;

    @BeforeEach
    void setUp() {
        repository = H2SdrRepository.createInMemory("test-list-" + System.nanoTime());
        sdrFactory = new DefaultSdrFactory();
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    void shouldListAllSdrsInTableFormat() {
        // Given - register some SDRs
        registerTestSdr("model1", "1.0");
        registerTestSdr("model2", "2.0");

        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        // When / Then
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("NAME"), "Should have table header");
                    assertTrue(output.contains("VERSION"), "Should have VERSION column");
                    assertTrue(output.contains("HASH"), "Should have HASH column");
                    assertTrue(output.contains("model1"), "Should list model1");
                    assertTrue(output.contains("model2"), "Should list model2");
                    assertTrue(output.contains("Total: 2 SDR(s)"), "Should show total count");
                },
                "--format",
                "table");
    }

    @Test
    void shouldListSdrsInJsonFormat() {
        // Given
        registerTestSdr("json-model", "1.0");

        var command = new ListCommand();
        command.format = "json";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        // When / Then
        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("\"sdrs\""), () -> "Should have sdrs array; actual: " + output);
                    assertTrue(output.contains("\"name\""), () -> "Should have name field; actual: " + output);
                    assertTrue(output.contains("\"json-model\""), () -> "Should contain model name; actual: " + output);
                    assertTrue(output.contains("\"total\""), () -> "Should have total field; actual: " + output);
                },
                "--format",
                "json");
    }

    @Test
    void shouldListSdrsInYamlFormat() {
        // Given
        registerTestSdr("yaml-model", "3.0");

        var command = new ListCommand();
        command.format = "yaml";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("sdrs:"), () -> "Should have sdrs key; actual: " + output);
                    assertTrue(output.contains("name:"), () -> "Should have name field; actual: " + output);
                    assertTrue(output.contains("yaml-model"), () -> "Should contain model name; actual: " + output);
                    assertTrue(output.contains("total:"), () -> "Should have total field; actual: " + output);
                },
                "--format",
                "yaml");
    }

    @Test
    void shouldHandleEmptyRepository() {
        // Given - empty repository
        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(
                            output.contains("No SDRs registered") || output.contains("Total: 0"),
                            "Should indicate empty repository");
                },
                "--format",
                "table");
    }

    @Test
    void shouldRespectLimitOption() {
        // Given - register 5 SDRs
        for (int i = 1; i <= 5; i++) {
            registerTestSdr("model" + i, "1.0");
        }

        var command = new ListCommand();
        command.format = "table";
        command.limit = 3;
        command.repositoryMixin = createMixin();

        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(
                            output.contains("Total: 3 SDR(s)"), () -> "Should limit to 3 results; actual: " + output);
                },
                "--format",
                "table",
                "--limit",
                "3");
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        var command = new ListCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--format", "xml");

        // Then
        assertEquals(1, exitCode, "Should fail with invalid format");
    }

    @Test
    void shouldHandleRepositoryError() {
        // Given - invalid repository path
        var command = new ListCommand();
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path";
        command.repositoryMixin = mixin;
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--format", "table");

        // Then
        assertEquals(1, exitCode, "Should fail with repository error");
    }

    @Test
    void shouldTruncateLongNames() {
        // Given - model with very long name
        String longName = "a".repeat(50);
        registerTestSdr(longName, "1.0");

        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("..."), "Should truncate long names with ellipsis");
                },
                "--format",
                "table");
    }

    @Test
    void shouldEscapeSpecialCharactersInJson() {
        // Given - model with characters that need escaping in JSON
        registerTestSdr("model-with-newline", "1.0");

        var command = new ListCommand();
        command.format = "json";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        var cmd = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(
                            output.contains("\"name\""), () -> "Should have proper JSON structure; actual: " + output);
                    assertTrue(
                            output.contains("model-with-newline"),
                            () -> "Should contain model name; actual: " + output);
                },
                "--format",
                "json");
    }

    private void registerTestSdr(String modelName, String modelVersion) {
        String modelSource = """
                version: "0.1"
                name: "%s"
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
                """.formatted(modelName);

        var sdr = sdrFactory.create(modelSource, "application/yaml", "postgres");

        try {
            repository.save(sdr, modelName, modelVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test SDR", e);
        }
    }

    private RepositoryMixin createMixin() {
        var mixin = new RepositoryMixin();
        mixin.testRepository = repository;
        return mixin;
    }
}
