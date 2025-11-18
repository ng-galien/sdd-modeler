package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        command.identifier = testHash;
        command.format = "all";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
        assertTrue(output.contains("Schema Hash:"), "Should show schema hash");
        assertTrue(output.contains("=== Schema (JSON) ==="), "Should show schema");
        assertTrue(output.contains("=== DDL (SQL) ==="), "Should show DDL");
    }

    @Test
    void shouldShowSdrByPartialHashNotSupported() {
        // Given - short hash is not currently supported (requires full hash)
        String shortHash = testHash.substring(0, 8);

        var command = new ShowCommand();
        command.identifier = shortHash;
        command.format = "all";
        command.repositoryMixin = createMixin();

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Short hash lookup not yet supported - should fail with exit code 1");
    }

    @Test
    void shouldShowSdrByName() {
        // Given
        var command = new ShowCommand();
        command.identifier = "test-model";
        command.format = "all";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
    }

    @Test
    void shouldShowSdrByNameAndVersion() {
        // Given
        var command = new ShowCommand();
        command.identifier = "test-model:1.0";
        command.format = "all";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
    }

    @Test
    void shouldShowMetadataOnly() {
        // Given
        var command = new ShowCommand();
        command.identifier = testHash;
        command.format = "metadata";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
        assertFalse(output.contains("=== Schema (JSON) ==="), "Should not show schema");
        assertFalse(output.contains("=== DDL (SQL) ==="), "Should not show DDL");
    }

    @Test
    void shouldShowSchemaOnly() {
        // Given
        var command = new ShowCommand();
        command.identifier = testHash;
        command.format = "schema";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== Schema (JSON) ==="), "Should show schema header");
        assertFalse(output.contains("=== SDR Metadata ==="), "Should not show metadata");
        assertFalse(output.contains("=== DDL (SQL) ==="), "Should not show DDL");
    }

    @Test
    void shouldShowDdlOnly() {
        // Given
        var command = new ShowCommand();
        command.identifier = testHash;
        command.format = "ddl";
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("=== DDL (SQL) ==="), "Should show DDL header");
        assertFalse(output.contains("=== SDR Metadata ==="), "Should not show metadata");
        assertFalse(output.contains("=== Schema (JSON) ==="), "Should not show schema");
    }

    @Test
    void shouldHandleNonExistentHash() {
        // Given
        var command = new ShowCommand();
        command.identifier = "0000000000000000";
        command.format = "all";
        command.repositoryMixin = createMixin();

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for nonexistent SDR");
    }

    @Test
    void shouldHandleNonExistentName() {
        // Given
        var command = new ShowCommand();
        command.identifier = "nonexistent-model";
        command.format = "all";
        command.repositoryMixin = createMixin();

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Should fail with exit code 1 for nonexistent model");
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        var command = new ShowCommand();
        command.identifier = testHash;
        command.format = "xml"; // Invalid format
        command.repositoryMixin = createMixin();

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Should fail with invalid format");
    }

    @Test
    void shouldHandleRepositoryError() {
        // Given
        var command = new ShowCommand();
        command.identifier = testHash;
        command.format = "all";

        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path";
        command.repositoryMixin = mixin;

        // When
        int exitCode = command.call();

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

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode, "Should find latest version");

        String output = outContent.toString();
        assertTrue(output.contains("=== SDR Metadata ==="), "Should show metadata");
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
