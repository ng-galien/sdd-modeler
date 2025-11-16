package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for {@link MigrateCommand}.
 *
 * <p>Note: Tests requiring actual LLM execution are disabled as they need either:
 * - Jlama model download (~1GB+)
 * - Ollama server running locally
 * These tests validate CLI argument parsing, repository integration, and error handling.
 */
class MigrateCommandTest {

    private H2SdrRepository repository;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Use in-memory database for faster tests
        repository = H2SdrRepository.createInMemory("test-migrate-" + System.nanoTime());

        // Capture stdout/stderr
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.close();
        }
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void shouldFailWhenSourceSdrNotFound() {
        // Given
        var cmd = new MigrateCommand();
        cmd.fromIdentifier = "nonexistent";
        cmd.toIdentifier = "hash2";
        cmd.dialect = "postgres";
        cmd.repositoryMixin = new RepositoryMixin();
        cmd.repositoryMixin.testRepository = repository;

        // When
        int exitCode = cmd.call();

        // Then
        assertEquals(1, exitCode);
        String output = errContent.toString();
        assertTrue(output.contains("ERROR"));
        assertTrue(output.contains("Source SDR not found"));
    }

    @Test
    void shouldFailWhenTargetSdrNotFound() {
        // Given
        var sdr1 = createTestSdr("hash1", "CREATE TABLE test;");
        repository.save(sdr1, "test-model", "1.0").get();

        var cmd = new MigrateCommand();
        cmd.fromIdentifier = "hash1";
        cmd.toIdentifier = "nonexistent";
        cmd.dialect = "postgres";
        cmd.repositoryMixin = new RepositoryMixin();
        cmd.repositoryMixin.testRepository = repository;

        // When
        int exitCode = cmd.call();

        // Then
        assertEquals(1, exitCode);
        String output = errContent.toString();
        assertTrue(output.contains("ERROR"));
        assertTrue(output.contains("Target SDR not found"));
    }

    @Test
    void shouldRejectUnsupportedDialect() {
        // Given
        var sdr1 = createTestSdr("hash1", "CREATE TABLE test;");
        var sdr2 = createTestSdr("hash2", "CREATE TABLE test2;");
        repository.save(sdr1, "test-model", "1.0").get();
        repository.save(sdr2, "test-model", "2.0").get();

        var cmd = new MigrateCommand();
        cmd.fromIdentifier = "hash1";
        cmd.toIdentifier = "hash2";
        cmd.dialect = "mysql"; // Unsupported
        cmd.repositoryMixin = new RepositoryMixin();
        cmd.repositoryMixin.testRepository = repository;

        // When
        int exitCode = cmd.call();

        // Then
        assertEquals(1, exitCode);
        String output = errContent.toString();
        assertTrue(output.contains("ERROR"));
        assertTrue(output.contains("Unsupported dialect"));
    }

    @Test
    void shouldReuseExistingMigrationByDefault() {
        // Given - create migration first
        var sdr1 = createTestSdr("hash1", "CREATE TABLE test;");
        var sdr2 = createTestSdr("hash2", "CREATE TABLE test2;");
        repository.save(sdr1, "test-model", "1.0").get();
        repository.save(sdr2, "test-model", "2.0").get();

        var migration = new io.statemodeler.repository.SdrMigration(
                "hash1", "hash2", "-- Existing migration", "postgres", java.time.Instant.now());
        repository.saveMigration(migration).get();

        var cmd = new MigrateCommand();
        cmd.fromIdentifier = "hash1";
        cmd.toIdentifier = "hash2";
        cmd.dialect = "postgres";
        cmd.repositoryMixin = new RepositoryMixin();
        cmd.repositoryMixin.testRepository = repository;

        // When
        int exitCode = cmd.call();

        // Then
        assertEquals(0, exitCode);
        String output = errContent.toString();
        assertTrue(output.contains("Migration already exists"));
    }

    @Test
    void shouldHaveMigrateSubcommandInMain() {
        // Given
        var main = new Main();
        var cmd = new CommandLine(main);

        // When
        var subcommands = cmd.getSubcommands();

        // Then
        assertTrue(subcommands.containsKey("migrate"));
    }

    private SdrRecord createTestSdr(String hash, String ddl) {
        String schema = "{\"name\":\"test\"}";
        return new SdrRecord(schema, "application/json", ddl, hash, "ddl-hash-" + hash, "1.0.0");
    }
}
