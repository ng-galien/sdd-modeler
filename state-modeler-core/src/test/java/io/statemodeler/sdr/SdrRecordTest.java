package io.statemodeler.sdr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SdrRecordTest {

    @Test
    void shouldCreateValidSdrRecord() {
        // Given
        String schema = "{\"version\":\"0.1\"}";
        String contentType = "application/yaml";
        String ddl = "CREATE TABLE test";
        String hash = "abc123";
        String version = "1.0.0";

        // When
        var sdr = new SdrRecord(schema, contentType, ddl, hash, version);

        // Then
        assertEquals(schema, sdr.schema());
        assertEquals(contentType, sdr.contentType());
        assertEquals(ddl, sdr.ddl());
        assertEquals(hash, sdr.hash());
        assertEquals(version, sdr.version());
    }

    @Test
    void shouldRejectNullSchema() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord(null, "application/yaml", "CREATE TABLE", "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("schema"));
    }

    @Test
    void shouldRejectBlankSchema() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("  ", "application/yaml", "CREATE TABLE", "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("schema"));
    }

    @Test
    void shouldRejectNullContentType() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> new SdrRecord("{}", null, "CREATE TABLE", "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("contentType"));
    }

    @Test
    void shouldRejectNullDdl() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> new SdrRecord("{}", "application/yaml", null, "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("ddl"));
    }

    @Test
    void shouldRejectNullHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", null, "1.0.0"));
        assertTrue(exception.getMessage().contains("hash"));
    }

    @Test
    void shouldRejectNullVersion() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", null));
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void shouldRejectBlankContentType() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> new SdrRecord("{}", "  ", "CREATE TABLE", "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("contentType"));
    }

    @Test
    void shouldRejectBlankDdl() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> new SdrRecord("{}", "application/yaml", "  ", "hash", "1.0.0"));
        assertTrue(exception.getMessage().contains("ddl"));
    }

    @Test
    void shouldRejectBlankHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "  ", "1.0.0"));
        assertTrue(exception.getMessage().contains("hash"));
    }

    @Test
    void shouldRejectBlankVersion() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", "  "));
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void shouldComputeBuildFingerprint() {
        // Given
        String ddl = "CREATE TABLE test (id INT)";
        String modelHash = "model123";
        String version = "1.0.0";

        var sdr = new SdrRecord("{}", "application/yaml", ddl, modelHash, version);

        // When
        String fingerprint = sdr.buildFingerprint();

        // Then
        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length()); // SHA-256 hash
        assertTrue(fingerprint.matches("^[0-9a-f]{64}$"));
    }

    @Test
    void shouldProduceDeterministicFingerprints() {
        // Given
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", "1.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertEquals(fp1, fp2);
    }

    @Test
    void shouldProduceDifferentFingerprintsForDifferentDdl() {
        // Given
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t1", "hash", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t2", "hash", "1.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertNotEquals(fp1, fp2);
    }

    @Test
    void shouldProduceDifferentFingerprintsForDifferentVersion() {
        // Given
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash", "2.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertNotEquals(fp1, fp2);
    }
}
