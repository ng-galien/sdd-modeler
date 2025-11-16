package io.statemodeler.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SdrMetadata}.
 */
class SdrMetadataTest {

    @Test
    void shouldCreateValidMetadata() {
        // Given
        String hash = "222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4";
        String name = "test-model";
        String version = "1.0.0";
        String sdrVersion = "1.0.0";
        String fingerprint = "fingerprint123";
        Instant now = Instant.now();

        // When
        var metadata = new SdrMetadata(hash, name, version, sdrVersion, fingerprint, now);

        // Then
        assertEquals(hash, metadata.schemaHash());
        assertEquals(name, metadata.modelName());
        assertEquals(version, metadata.modelVersion());
        assertEquals(sdrVersion, metadata.sdrVersion());
        assertEquals(fingerprint, metadata.buildFingerprint());
        assertEquals(now, metadata.createdAt());
    }

    @Test
    void shouldReturnShortHash() {
        // Given
        String hash = "222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4";
        var metadata = new SdrMetadata(hash, "model", "1.0", "1.0.0", "fingerprint", Instant.now());

        // When
        String shortHash = metadata.shortHash();

        // Then
        assertEquals("222fa0d3", shortHash);
        assertEquals(8, shortHash.length());
    }

    @Test
    void shouldHandleShortHashWithShortInput() {
        // Given - hash shorter than 8 characters
        String hash = "abc123";
        var metadata = new SdrMetadata(hash, "model", "1.0", "1.0.0", "fingerprint", Instant.now());

        // When
        String shortHash = metadata.shortHash();

        // Then
        assertEquals("abc123", shortHash);
        assertEquals(hash, shortHash);
    }

    @Test
    void shouldHandleShortHashWithExactly8Characters() {
        // Given - hash exactly 8 characters
        String hash = "12345678";
        var metadata = new SdrMetadata(hash, "model", "1.0", "1.0.0", "fingerprint", Instant.now());

        // When
        String shortHash = metadata.shortHash();

        // Then
        assertEquals("12345678", shortHash);
        assertEquals(hash, shortHash);
    }

    @Test
    void shouldRejectNullSchemaHash() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata(null, "model", "1.0", "1.0.0", "fingerprint", Instant.now()),
                "schemaHash cannot be null or blank");
    }

    @Test
    void shouldRejectBlankSchemaHash() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("  ", "model", "1.0", "1.0.0", "fingerprint", Instant.now()),
                "schemaHash cannot be null or blank");
    }

    @Test
    void shouldRejectEmptySchemaHash() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("", "model", "1.0", "1.0.0", "fingerprint", Instant.now()),
                "schemaHash cannot be null or blank");
    }

    @Test
    void shouldRejectNullModelName() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", null, "1.0", "1.0.0", "fingerprint", Instant.now()),
                "modelName cannot be null or blank");
    }

    @Test
    void shouldRejectBlankModelName() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "  ", "1.0", "1.0.0", "fingerprint", Instant.now()),
                "modelName cannot be null or blank");
    }

    @Test
    void shouldRejectEmptyModelName() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "", "1.0", "1.0.0", "fingerprint", Instant.now()),
                "modelName cannot be null or blank");
    }

    @Test
    void shouldRejectNullModelVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", null, "1.0.0", "fingerprint", Instant.now()),
                "modelVersion cannot be null or blank");
    }

    @Test
    void shouldRejectBlankModelVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "  ", "1.0.0", "fingerprint", Instant.now()),
                "modelVersion cannot be null or blank");
    }

    @Test
    void shouldRejectEmptyModelVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "", "1.0.0", "fingerprint", Instant.now()),
                "modelVersion cannot be null or blank");
    }

    @Test
    void shouldRejectNullSdrVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", null, "fingerprint", Instant.now()),
                "sdrVersion cannot be null or blank");
    }

    @Test
    void shouldRejectBlankSdrVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "  ", "fingerprint", Instant.now()),
                "sdrVersion cannot be null or blank");
    }

    @Test
    void shouldRejectEmptySdrVersion() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "", "fingerprint", Instant.now()),
                "sdrVersion cannot be null or blank");
    }

    @Test
    void shouldRejectNullBuildFingerprint() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "1.0.0", null, Instant.now()),
                "buildFingerprint cannot be null or blank");
    }

    @Test
    void shouldRejectBlankBuildFingerprint() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "1.0.0", "  ", Instant.now()),
                "buildFingerprint cannot be null or blank");
    }

    @Test
    void shouldRejectEmptyBuildFingerprint() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "1.0.0", "", Instant.now()),
                "buildFingerprint cannot be null or blank");
    }

    @Test
    void shouldRejectNullCreatedAt() {
        // When/Then
        assertThrows(
                IllegalArgumentException.class,
                () -> new SdrMetadata("hash123", "model", "1.0", "1.0.0", "fingerprint", null),
                "createdAt cannot be null");
    }

    @Test
    void shouldSupportRecordEquality() {
        // Given
        Instant now = Instant.now();
        var metadata1 = new SdrMetadata("hash123", "model", "1.0", "1.0.0", "fingerprint", now);
        var metadata2 = new SdrMetadata("hash123", "model", "1.0", "1.0.0", "fingerprint", now);
        var metadata3 = new SdrMetadata("hash456", "model", "1.0", "1.0.0", "fingerprint", now);

        // When/Then
        assertEquals(metadata1, metadata2, "Same values should be equal");
        assertNotEquals(metadata1, metadata3, "Different hash should not be equal");
        assertEquals(metadata1.hashCode(), metadata2.hashCode(), "Same hashCode for equal objects");
    }

    @Test
    void shouldSupportRecordToString() {
        // Given
        Instant now = Instant.parse("2024-11-16T10:30:00Z");
        var metadata = new SdrMetadata("hash123", "model", "1.0", "1.0.0", "fingerprint", now);

        // When
        String toString = metadata.toString();

        // Then
        assertTrue(toString.contains("hash123"), "toString should contain hash");
        assertTrue(toString.contains("model"), "toString should contain model name");
        assertTrue(toString.contains("1.0"), "toString should contain version");
        assertTrue(toString.contains("fingerprint"), "toString should contain fingerprint");
    }
}
