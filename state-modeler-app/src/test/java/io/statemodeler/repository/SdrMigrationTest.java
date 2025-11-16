package io.statemodeler.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SdrMigration} record.
 */
class SdrMigrationTest {

    @Test
    void shouldCreateValidMigration() {
        // Given
        var now = Instant.now();

        // When
        var migration = new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "postgres", now);

        // Then
        assertEquals("hash1", migration.fromHash());
        assertEquals("hash2", migration.toHash());
        assertEquals("ALTER TABLE test;", migration.migrationScript());
        assertEquals("postgres", migration.dialect());
        assertEquals(now, migration.createdAt());
    }

    @Test
    void shouldRejectNullFromHash() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration(null, "hash2", "ALTER TABLE test;", "postgres", Instant.now()));
        assertEquals("fromHash cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankFromHash() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("", "hash2", "ALTER TABLE test;", "postgres", Instant.now()));
        assertEquals("fromHash cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectNullToHash() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", null, "ALTER TABLE test;", "postgres", Instant.now()));
        assertEquals("toHash cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankToHash() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "", "ALTER TABLE test;", "postgres", Instant.now()));
        assertEquals("toHash cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectNullMigrationScript() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "hash2", null, "postgres", Instant.now()));
        assertEquals("migrationScript cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankMigrationScript() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "hash2", "   ", "postgres", Instant.now()));
        assertEquals("migrationScript cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectNullDialect() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "hash2", "ALTER TABLE test;", null, Instant.now()));
        assertEquals("dialect cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankDialect() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "", Instant.now()));
        assertEquals("dialect cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectNullCreatedAt() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "postgres", null));
        assertEquals("createdAt cannot be null", exception.getMessage());
    }

    @Test
    void shouldSupportRecordEquality() {
        // Given
        var now = Instant.now();
        var migration1 = new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "postgres", now);
        var migration2 = new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "postgres", now);
        var migration3 = new SdrMigration("hash1", "hash3", "ALTER TABLE test;", "postgres", now);

        // Then
        assertEquals(migration1, migration2);
        assertNotEquals(migration1, migration3);
    }

    @Test
    void shouldSupportRecordToString() {
        // Given
        var migration = new SdrMigration("hash1", "hash2", "ALTER TABLE test;", "postgres", Instant.now());

        // Then
        String str = migration.toString();
        assertTrue(str.contains("hash1"));
        assertTrue(str.contains("hash2"));
        assertTrue(str.contains("postgres"));
    }
}
