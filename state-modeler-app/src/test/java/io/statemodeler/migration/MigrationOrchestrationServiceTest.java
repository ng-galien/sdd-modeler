package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MigrationOrchestrationService}.
 */
class MigrationOrchestrationServiceTest {

    private H2SdrRepository repository;
    private MigrationOrchestrationService service;
    private MigrationGenerationService mockMigrationGenerator;

    @BeforeEach
    void setUp() {
        // Use in-memory database for faster tests
        repository = H2SdrRepository.createInMemory("test-orchestration-" + System.nanoTime());

        // Create a simple mock migration generator that returns MigrationResult
        mockMigrationGenerator = (oldDdl, newDdl, textDiff, dialect) -> io.vavr.control.Try.success(new MigrationResult(
                0.9,
                "-- Migration from v1 to v2\nALTER TABLE users ADD COLUMN email VARCHAR(255);",
                "Added email column to users table"));

        var comparisonService = new DdlComparisonService();
        service = new MigrationOrchestrationService(mockMigrationGenerator, comparisonService, repository);
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    void shouldGenerateAndSaveMigration() {
        // Given
        var fromSdr = createTestSdr("hash1", "CREATE TABLE users (id INT);");
        var toSdr = createTestSdr("hash2", "CREATE TABLE users (id INT, email VARCHAR(255));");

        repository.save(fromSdr, "test-model", "1.0").get();
        repository.save(toSdr, "test-model", "2.0").get();

        // When
        var result = service.generateAndSaveMigration(fromSdr, toSdr, "postgres");

        // Then
        assertTrue(result.isSuccess());
        var migration = result.get();
        assertEquals("hash1", migration.fromHash());
        assertEquals("hash2", migration.toHash());
        assertTrue(migration.migrationScript().contains("ALTER TABLE"));
        assertEquals("postgres", migration.dialect());

        // Verify it was persisted
        var findResult = repository.findMigration("hash1", "hash2");
        assertTrue(findResult.isSuccess());
        assertTrue(findResult.get().isPresent());
    }

    @Test
    void shouldIncludeDiffInPrompt() {
        // Given - capture textDiff passed into migration generator
        java.util.concurrent.atomic.AtomicReference<String> capturedDiff =
                new java.util.concurrent.atomic.AtomicReference<>();

        var capturingGenerator = (MigrationGenerationService) (oldDdl, newDdl, textDiff, dialect) -> {
            capturedDiff.set(textDiff);
            return io.vavr.control.Try.success(new MigrationResult(0.9, "-- Migration script", "Added email column"));
        };

        var comparisonService = new DdlComparisonService();
        var svc = new MigrationOrchestrationService(capturingGenerator, comparisonService, repository);

        var fromSdr = createTestSdr("hash1", "CREATE TABLE users (id INT);");
        var toSdr = createTestSdr("hash2", "CREATE TABLE users (id INT, email VARCHAR(255));");

        repository.save(fromSdr, "test-model", "1.0").get();
        repository.save(toSdr, "test-model", "2.0").get();

        // When
        var result = svc.generateAndSaveMigration(fromSdr, toSdr, "postgres");

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(capturedDiff.get());
        assertTrue(capturedDiff.get().contains("email VARCHAR(255)"));
    }

    @Test
    void shouldRejectNullFromSdr() {
        // Given
        var toSdr = createTestSdr("hash2", "CREATE TABLE test;");

        // When
        var result = service.generateAndSaveMigration(null, toSdr, "postgres");

        // Then
        assertTrue(result.isFailure());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertEquals("fromSdr cannot be null", result.getCause().getMessage());
    }

    @Test
    void shouldRejectNullToSdr() {
        // Given
        var fromSdr = createTestSdr("hash1", "CREATE TABLE test;");

        // When
        var result = service.generateAndSaveMigration(fromSdr, null, "postgres");

        // Then
        assertTrue(result.isFailure());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertEquals("toSdr cannot be null", result.getCause().getMessage());
    }

    @Test
    void shouldRejectNullDialect() {
        // Given
        var fromSdr = createTestSdr("hash1", "CREATE TABLE test;");
        var toSdr = createTestSdr("hash2", "CREATE TABLE test2;");

        // When
        var result = service.generateAndSaveMigration(fromSdr, toSdr, null);

        // Then
        assertTrue(result.isFailure());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertEquals("dialect cannot be null or blank", result.getCause().getMessage());
    }

    @Test
    void shouldRejectBlankDialect() {
        // Given
        var fromSdr = createTestSdr("hash1", "CREATE TABLE test;");
        var toSdr = createTestSdr("hash2", "CREATE TABLE test2;");

        // When
        var result = service.generateAndSaveMigration(fromSdr, toSdr, "   ");

        // Then
        assertTrue(result.isFailure());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void shouldHandleMigrationGeneratorFailure() {
        // Given - mock that fails
        var failingGenerator = (MigrationGenerationService) (oldDdl, newDdl, textDiff, dialect) ->
                io.vavr.control.Try.failure(new RuntimeException("LLM service unavailable"));

        var comparisonService = new DdlComparisonService();
        var failingService = new MigrationOrchestrationService(failingGenerator, comparisonService, repository);

        var fromSdr = createTestSdr("hash1", "CREATE TABLE test;");
        var toSdr = createTestSdr("hash2", "CREATE TABLE test2;");

        repository.save(fromSdr, "test-model", "1.0").get();
        repository.save(toSdr, "test-model", "2.0").get();

        // When
        var result = failingService.generateAndSaveMigration(fromSdr, toSdr, "postgres");

        // Then
        assertTrue(result.isFailure());
        assertEquals("LLM service unavailable", result.getCause().getMessage());
    }

    @Test
    void shouldFindExistingMigration() {
        // Given
        var fromSdr = createTestSdr("hash1", "CREATE TABLE test;");
        var toSdr = createTestSdr("hash2", "CREATE TABLE test2;");

        repository.save(fromSdr, "test-model", "1.0").get();
        repository.save(toSdr, "test-model", "2.0").get();

        service.generateAndSaveMigration(fromSdr, toSdr, "postgres").get();

        // When
        var result = service.findMigration("hash1", "hash2");

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.get().isPresent());

        var migration = result.get().get();
        assertEquals("hash1", migration.fromHash());
        assertEquals("hash2", migration.toHash());
    }

    @Test
    void shouldReturnEmptyWhenMigrationNotFound() {
        // When
        var result = service.findMigration("nonexistent1", "nonexistent2");

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void shouldRejectNullMigrationGenerator() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MigrationOrchestrationService(null, new DdlComparisonService(), repository));
        assertEquals("migrationGenerator cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullComparisonService() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MigrationOrchestrationService(mockMigrationGenerator, null, repository));
        assertEquals("comparisonService cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullRepository() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MigrationOrchestrationService(mockMigrationGenerator, new DdlComparisonService(), null));
        assertEquals("repository cannot be null", exception.getMessage());
    }

    private SdrRecord createTestSdr(String hash, String ddl) {
        String schema = "{\"name\":\"test\"}";
        return new SdrRecord(schema, "application/json", ddl, hash, "ddl-hash-" + hash, "1.0.0");
    }
}
