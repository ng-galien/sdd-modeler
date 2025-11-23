package io.statemodeler.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.sdr.SdrRecord;
import org.junit.jupiter.api.*;

/**
 * Integration tests for {@link H2SdrRepository}.
 *
 * <p>Uses an in-memory H2 database for fast test execution.
 */
class H2SdrRepositoryTest {

    private H2SdrRepository repository;

    @BeforeEach
    void setUp() {
        // Use in-memory database for faster tests
        repository = H2SdrRepository.createInMemory("test-repo-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() {
        // Close repository
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    void shouldSaveAndFindByHash() {
        // Given
        var sdr = createTestSdr("hash1", "schema1", "ddl1");

        // When
        var saveResult = repository.save(sdr, "test-model", "1.0.0");
        var findResult = repository.findByHash("hash1");

        // Then
        assertTrue(saveResult.isSuccess());
        assertTrue(findResult.isSuccess());

        var found = findResult.get();
        assertTrue(found.isPresent());
        assertEquals("hash1", found.get().schemaHash());
        assertEquals("schema1", found.get().schema());
        assertEquals("ddl1", found.get().ddl());
    }

    @Test
    void shouldReturnEmptyWhenHashNotFound() {
        // When
        var result = repository.findByHash("nonexistent");

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void shouldFailOnDuplicateHash() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash1", "schema2", "ddl2");

        repository.save(sdr1, "model1", "1.0.0");

        // When
        var result = repository.save(sdr2, "model2", "2.0.0");

        // Then
        assertTrue(result.isFailure());
        assertInstanceOf(IllegalArgumentException.class, result.getCause());
        assertTrue(result.getCause().getMessage().contains("already exists"));
    }

    @Test
    void shouldFindByName() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");
        var sdr3 = createTestSdr("hash3", "schema3", "ddl3");

        repository.save(sdr1, "orders", "1.0.0");
        repository.save(sdr2, "orders", "1.1.0");
        repository.save(sdr3, "users", "1.0.0");

        // When
        var result = repository.findByName("orders");

        // Then
        assertTrue(result.isSuccess());
        var metadata = result.get();
        assertEquals(2, metadata.size());
        assertTrue(metadata.stream().allMatch(m -> m.modelName().equals("orders")));
        assertTrue(metadata.stream().map(SdrMetadata::schemaHash).anyMatch(h -> h.equals("hash1")));
        assertTrue(metadata.stream().map(SdrMetadata::schemaHash).anyMatch(h -> h.equals("hash2")));
    }

    @Test
    void shouldReturnEmptyListForNonexistentName() {
        // When
        var result = repository.findByName("nonexistent");

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void shouldFindByNameAndVersion() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");

        repository.save(sdr1, "orders", "1.0.0");
        repository.save(sdr2, "orders", "1.1.0");

        // When
        var result = repository.findByNameAndVersion("orders", "1.1.0");

        // Then
        assertTrue(result.isSuccess());
        var found = result.get();
        assertTrue(found.isPresent());
        assertEquals("hash2", found.get().schemaHash());
    }

    @Test
    void shouldReturnMostRecentForDuplicateNameAndVersion() throws InterruptedException {
        // Given - same model/version saved twice with different hashes
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        repository.save(sdr1, "orders", "1.0.0");

        Thread.sleep(10); // Ensure different timestamps

        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");
        repository.save(sdr2, "orders", "1.0.0");

        // When
        var result = repository.findByNameAndVersion("orders", "1.0.0");

        // Then - should return most recent (hash2)
        assertTrue(result.isSuccess());
        var found = result.get();
        assertTrue(found.isPresent());
        assertEquals("hash2", found.get().schemaHash());
    }

    @Test
    void shouldListAll() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");
        var sdr3 = createTestSdr("hash3", "schema3", "ddl3");

        repository.save(sdr1, "orders", "1.0.0");
        repository.save(sdr2, "users", "1.0.0");
        repository.save(sdr3, "products", "1.0.0");

        // When
        var result = repository.listAll();

        // Then
        assertTrue(result.isSuccess());
        var allMetadata = result.get();
        assertEquals(3, allMetadata.size());
        var names = allMetadata.stream().map(SdrMetadata::modelName).toList();
        assertTrue(names.contains("orders"));
        assertTrue(names.contains("users"));
        assertTrue(names.contains("products"));
    }

    @Test
    void shouldFindRecent() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");
        var sdr3 = createTestSdr("hash3", "schema3", "ddl3");

        repository.save(sdr1, "model1", "1.0.0");
        repository.save(sdr2, "model2", "1.0.0");
        repository.save(sdr3, "model3", "1.0.0");
        repository.save(sdr1, "model1", "1.0.0");
        repository.save(sdr2, "model2", "1.0.0");
        repository.save(sdr3, "model3", "1.0.0");

        // When
        var result = repository.findRecent(2);

        // Then
        assertTrue(result.isSuccess());
        var recent = result.get();
        assertEquals(2, recent.size());
        // Most recent should be first (DESC order)
        assertEquals("hash3", recent.get(0).schemaHash());
        assertEquals("hash2", recent.get(1).schemaHash());
    }

    @Test
    void shouldReturnEmptyListForZeroOrNegativeLimit() {
        // Given
        var sdr = createTestSdr("hash1", "schema1", "ddl1");
        repository.save(sdr, "model1", "1.0.0");

        // When
        var resultZero = repository.findRecent(0);
        var resultNegative = repository.findRecent(-5);

        // Then
        assertTrue(resultZero.isSuccess());
        assertTrue(resultZero.get().isEmpty());
        assertTrue(resultNegative.isSuccess());
        assertTrue(resultNegative.get().isEmpty());
    }

    @Test
    void shouldDeleteByHash() {
        // Given
        var sdr = createTestSdr("hash1", "schema1", "ddl1");
        repository.save(sdr, "test-model", "1.0.0");

        // When
        var deleteResult = repository.delete("hash1");
        var findResult = repository.findByHash("hash1");

        // Then
        assertTrue(deleteResult.isSuccess());
        assertTrue(deleteResult.get());
        assertTrue(findResult.get().isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonexistent() {
        // When
        var result = repository.delete("nonexistent");

        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.get());
    }

    @Test
    void shouldCountRecords() {
        // Given
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");

        repository.save(sdr1, "model1", "1.0.0");
        repository.save(sdr2, "model2", "1.0.0");

        // When
        var result = repository.count();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(2L, result.get());
    }

    @Test
    void shouldReturnZeroCountForEmptyRepository() {
        // When
        var result = repository.count();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(0L, result.get());
    }

    @Test
    void shouldCheckExistence() {
        // Given
        var sdr = createTestSdr("hash1", "schema1", "ddl1");
        repository.save(sdr, "test-model", "1.0.0");

        // When
        var existsResult = repository.exists("hash1");
        var notExistsResult = repository.exists("nonexistent");

        // Then
        assertTrue(existsResult.isSuccess());
        assertTrue(existsResult.get());
        assertTrue(notExistsResult.isSuccess());
        assertFalse(notExistsResult.get());
    }

    @Test
    void shouldHandleNullAndBlankParameters() {
        // When - save with null/blank
        var saveNullSdr = repository.save(null, "model", "1.0.0");
        var saveNullName = repository.save(createTestSdr("h1", "s1", "d1"), null, "1.0.0");
        var saveBlankName = repository.save(createTestSdr("h2", "s2", "d2"), "", "1.0.0");
        var saveNullVersion = repository.save(createTestSdr("h3", "s3", "d3"), "model", null);

        // When - find with null/blank
        var findNullHash = repository.findByHash(null);
        var findBlankHash = repository.findByHash("");
        var findNullName = repository.findByName(null);
        var findBlankName = repository.findByName("");

        // When - delete/exists with null/blank
        var deleteNull = repository.delete(null);
        var existsNull = repository.exists(null);

        // Then - save operations should fail
        assertTrue(saveNullSdr.isFailure());
        assertTrue(saveNullName.isFailure());
        assertTrue(saveBlankName.isFailure());
        assertTrue(saveNullVersion.isFailure());

        // Then - find operations should return empty
        assertTrue(findNullHash.isSuccess());
        assertTrue(findNullHash.get().isEmpty());
        assertTrue(findBlankHash.get().isEmpty());
        assertTrue(findNullName.get().isEmpty());
        assertTrue(findBlankName.get().isEmpty());

        // Then - delete/exists should return false
        assertFalse(deleteNull.get());
        assertFalse(existsNull.get());
    }

    @Test
    void shouldPreserveMetadataFields() {
        // Given
        var sdr = new SdrRecord(
                "test-schema-json", "application/json", "test-ddl-sql", "test-hash-123", "ddl-hash-456", "1.0.0");

        // When
        repository.save(sdr, "orders", "2.0.0");
        var metadata = repository.findByName("orders").get();

        // Then
        assertEquals(1, metadata.size());
        var meta = metadata.get(0);
        assertEquals("test-hash-123", meta.schemaHash());
        assertEquals("orders", meta.modelName());
        assertEquals("2.0.0", meta.modelVersion());
        assertEquals("1.0.0", meta.sdrVersion());
        assertEquals(sdr.buildFingerprint(), meta.buildFingerprint());
        assertNotNull(meta.createdAt());
    }

    @Test
    void shouldSortListAllByCreatedAtDescending() throws InterruptedException {
        // Given - save in order: sdr1, sdr2, sdr3
        var sdr1 = createTestSdr("hash1", "schema1", "ddl1");
        repository.save(sdr1, "model1", "1.0.0");

        Thread.sleep(10);
        var sdr2 = createTestSdr("hash2", "schema2", "ddl2");
        repository.save(sdr2, "model2", "1.0.0");

        Thread.sleep(10);
        var sdr3 = createTestSdr("hash3", "schema3", "ddl3");
        repository.save(sdr3, "model3", "1.0.0");

        // When
        var result = repository.listAll();

        // Then - should be in reverse order (DESC)
        assertEquals(3, result.get().size());
        assertEquals("hash3", result.get().get(0).schemaHash());
        assertEquals("hash2", result.get().get(1).schemaHash());
        assertEquals("hash1", result.get().get(2).schemaHash());
    }

    /**
     * Helper to create test SDR records.
     */
    private SdrRecord createTestSdr(String hash, String schema, String ddl) {
        return new SdrRecord(schema, "application/json", ddl, hash, "ddl-hash-" + hash, "1.0.0");
    }
}
