package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.CliTestHelper;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for {@link ShowCommand}.
 */
class ShowCommandTest {

    private H2SdrRepository repository;
    private SdrFactory sdrFactory;
    private String testHash;

    @BeforeEach
    void setUp() {
        repository = H2SdrRepository.createInMemory("test-show-" + System.nanoTime());
        sdrFactory = new DefaultSdrFactory();

        // Register a test SDR
        testHash = registerTestSdr("test-model", "1.0");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    void shouldShowSdrByFullHash() {
        // Given
        var command = new ShowCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        CliTestHelper.runWithCapture(
                cmd,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata. Output was:\n" + output);
                },
                testHash,
                "--format",
                "all");
    }

    @Test
    void shouldShowSdrByPartialHashNotSupported() {
        // Given - short hash is not currently supported (requires full hash)
        String shortHash = testHash.substring(0, 8);

        var command = new ShowCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(shortHash, "--format", "all");

        // Then
        assertEquals(1, exitCode, "Short hash lookup not yet supported - should fail with exit code 1");
    }

    @Test
    void shouldShowSdrByName() {
        // Given
        var command2 = new ShowCommand();
        command2.repositoryMixin = createMixin();
        var cmd2 = new CommandLine(command2);
        CliTestHelper.runWithCapture(
                cmd2,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
                },
                "test-model",
                "--format",
                "all");
    }

    @Test
    void shouldShowSdrByNameAndVersion() {
        // Given
        var command3 = new ShowCommand();
        command3.repositoryMixin = createMixin();
        var cmd3 = new CommandLine(command3);
        CliTestHelper.runWithCapture(
                cmd3,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output3 = result.out();
                    assertTrue(
                            output3.contains("=== SDR Metadata ==="), "Should show metadata. Output was:\n" + output3);
                },
                "test-model:1.0",
                "--format",
                "all");
    }

    @Test
    void shouldShowMetadataOnly() {
        // Given
        var command4 = new ShowCommand();
        command4.repositoryMixin = createMixin();
        var cmd4 = new CommandLine(command4);
        CliTestHelper.runWithCapture(
                cmd4,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    System.out.println("DEBUG_IDENTITY_CMD: " + System.identityHashCode(cmd4));
                    assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata. Output was:\n" + output);
                    assertFalse(output.contains("=== Schema (JSON) ==="), "Should not show schema");
                    assertFalse(output.contains("=== DDL (SQL) ==="), "Should not show DDL");
                },
                testHash,
                "--format",
                "metadata");
    }

    @Test
    void shouldShowSchemaOnly() {
        // Given
        var command5 = new ShowCommand();
        command5.repositoryMixin = createMixin();
        var cmd5 = new CommandLine(command5);
        CliTestHelper.runWithCapture(
                cmd5,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("=== Schema (JSON) ==="), "Should show schema header");
                    assertFalse(output.contains("=== SDR Metadata ==="), "Should not show metadata");
                    assertFalse(output.contains("=== DDL (SQL) ==="), "Should not show DDL");
                },
                testHash,
                "--format",
                "schema");
    }

    @Test
    void shouldShowDdlOnly() {
        // Given
        var command6 = new ShowCommand();
        command6.repositoryMixin = createMixin();
        var cmd6 = new CommandLine(command6);
        CliTestHelper.runWithCapture(
                cmd6,
                result -> {
                    assertEquals(0, result.exitCode());
                    String output = result.out();
                    assertTrue(output.contains("=== DDL (SQL) ==="), "Should show DDL header");
                    assertFalse(output.contains("=== SDR Metadata ==="), "Should not show metadata");
                    assertFalse(output.contains("=== Schema (JSON) ==="), "Should not show schema");
                },
                testHash,
                "--format",
                "ddl");
    }

    @Test
    void shouldHandleNonExistentHash() {
        // Given
        var command = new ShowCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("0000000000000000", "--format", "all");

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for nonexistent SDR");
    }

    @Test
    void shouldHandleNonExistentName() {
        // Given
        var command = new ShowCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("nonexistent-model", "--format", "all");

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for nonexistent model");
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        var command = new ShowCommand();
        command.repositoryMixin = createMixin();
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(testHash, "--format", "xml");

        // Then
        assertEquals(1, exitCode, "Should fail with invalid format");
    }

    @Test
    void shouldHandleRepositoryError() {
        // Given
        var command = new ShowCommand();
        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path";
        command.repositoryMixin = mixin;
        var cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(testHash, "--format", "all");

        // Then
        assertEquals(1, exitCode, "Should fail with repository error");
    }

    @Test
    void shouldFindLatestVersionByName() {
        // Given - register multiple versions
        registerTestSdr("multi-version", "1.0");
        registerTestSdr("multi-version", "2.0");

        var command = new ShowCommand();
        command.identifier = "multi-version"; // No version specified
        command.format = "metadata";
        command.repositoryMixin = createMixin();

        var cmd7 = new CommandLine(command);
        CliTestHelper.runWithCapture(
                cmd7,
                result -> {
                    assertEquals(0, result.exitCode(), "Should find latest version");
                    String output = result.out();
                    assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
                },
                "multi-version",
                "--format",
                "metadata");
    }

    private String registerTestSdr(String modelName, String modelVersion) {
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

        return sdr.schemaHash();
    }

    private RepositoryMixin createMixin() {
        var mixin = new RepositoryMixin();
        mixin.testRepository = repository;
        return mixin;
    }
}
