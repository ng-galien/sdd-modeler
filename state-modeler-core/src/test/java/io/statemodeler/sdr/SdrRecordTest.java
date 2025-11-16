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
        String schemaHash = "abc123";
        String ddlHash = "def456";
        String version = "1.0.0";

        // When
        var sdr = new SdrRecord(schema, contentType, ddl, schemaHash, ddlHash, version);

        // Then
        assertEquals(schema, sdr.schema());
        assertEquals(contentType, sdr.contentType());
        assertEquals(ddl, sdr.ddl());
        assertEquals(schemaHash, sdr.schemaHash());
        assertEquals(ddlHash, sdr.ddlHash());
        assertEquals(version, sdr.version());
    }

    @Test
    void shouldRejectNullSchema() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord(null, "application/yaml", "CREATE TABLE", "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("schema"));
    }

    @Test
    void shouldRejectBlankSchema() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("  ", "application/yaml", "CREATE TABLE", "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("schema"));
    }

    @Test
    void shouldRejectNullContentType() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", null, "CREATE TABLE", "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("contentType"));
    }

    @Test
    void shouldRejectNullDdl() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", null, "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("ddl"));
    }

    @Test
    void shouldRejectNullHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", null, "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("schemaHash"));
    }

    @Test
    void shouldRejectNullVersion() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", null));
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void shouldRejectBlankContentType() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "  ", "CREATE TABLE", "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("contentType"));
    }

    @Test
    void shouldRejectBlankDdl() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "  ", "hash1", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("ddl"));
    }

    @Test
    void shouldRejectBlankHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "  ", "hash2", "1.0.0"));
        assertTrue(exception.getMessage().contains("schemaHash"));
    }

    @Test
    void shouldRejectBlankVersion() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", "  "));
        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void shouldComputeBuildFingerprint() {
        // Given
        String ddl = "CREATE TABLE test (id INT)";
        String schemaHash = "schema123";
        String ddlHash = "ddl456";
        String version = "1.0.0";

        var sdr = new SdrRecord("{}", "application/yaml", ddl, schemaHash, ddlHash, version);

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
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", "1.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertEquals(fp1, fp2);
    }

    @Test
    void shouldProduceDifferentFingerprintsForDifferentDdl() {
        // Given
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t1", "hash1", "ddlhash1", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t2", "hash1", "ddlhash2", "1.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertNotEquals(fp1, fp2);
    }

    @Test
    void shouldProduceDifferentFingerprintsForDifferentVersion() {
        // Given
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", "1.0.0");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "hash2", "2.0.0");

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then
        assertNotEquals(fp1, fp2);
    }

    @Test
    void shouldAvoidHashCollisionWithDelimiter() {
        // Given - potential collision scenario without delimiter
        var sdr1 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t1", "abc", "hash1", "1");
        var sdr2 = new SdrRecord("{}", "application/yaml", "CREATE TABLE t123", "ab", "hash2", "c1");
        // Without delimiter: "abc" + "hash1" + "1"
        //                vs: "ab"  + "hash2" + "c1"
        // Could collide if hash results align

        // When
        String fp1 = sdr1.buildFingerprint();
        String fp2 = sdr2.buildFingerprint();

        // Then - should produce different fingerprints due to delimiter
        assertNotEquals(fp1, fp2, "Delimiter should prevent hash collision");
    }

    @Test
    void shouldComputeCombinedHash() {
        // Given
        String schemaHash = "abc123";
        String ddlHash = "def456";
        var sdr = new SdrRecord("{}", "application/yaml", "CREATE TABLE", schemaHash, ddlHash, "1.0.0");

        // When
        String combined = sdr.combinedHash();

        // Then
        assertEquals("abc123def456", combined);
        assertEquals(schemaHash + ddlHash, combined);
    }

    @Test
    void shouldRejectNullDdlHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", null, "1.0.0"));
        assertTrue(exception.getMessage().contains("ddlHash"));
    }

    @Test
    void shouldRejectBlankDdlHash() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SdrRecord("{}", "application/yaml", "CREATE TABLE", "hash1", "  ", "1.0.0"));
        assertTrue(exception.getMessage().contains("ddlHash"));
    }
}
