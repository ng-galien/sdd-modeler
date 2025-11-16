package io.statemodeler.sdr;

/**
 * State Definition Record - Immutable snapshot of an SDD model with generated artifacts.
 *
 * <p>Represents a stable, versioned snapshot of:
 * <ul>
 *   <li>The normalized SDD model (schema)</li>
 *   <li>The generated DDL for a specific dialect</li>
 *   <li>A cryptographic hash ensuring integrity</li>
 * </ul>
 *
 * <p>Hashing is format-independent: identical models produce identical hashes regardless of:
 * <ul>
 *   <li>Input format (YAML vs JSON)</li>
 *   <li>Field ordering</li>
 *   <li>Whitespace and indentation</li>
 * </ul>
 *
 * @param schema the normalized SDD model in canonical JSON format
 * @param contentType the content type of the original input (e.g., "application/yaml", "application/json")
 * @param ddl the generated DDL SQL for the model
 * @param hash SHA-256 hash of the canonical model representation
 * @param version the version of the SDR format and generator used
 */
public record SdrRecord(String schema, String contentType, String ddl, String hash, String version) {

    /**
     * Creates a new SdrRecord with validation.
     *
     * @param schema the normalized SDD model (non-null, non-empty)
     * @param contentType the content type (non-null, non-empty)
     * @param ddl the generated DDL (non-null, non-empty)
     * @param hash the SHA-256 hash (non-null, non-empty)
     * @param version the SDR version (non-null, non-empty)
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public SdrRecord {
        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("schema cannot be null or blank");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType cannot be null or blank");
        }
        if (ddl == null || ddl.isBlank()) {
            throw new IllegalArgumentException("ddl cannot be null or blank");
        }
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("hash cannot be null or blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version cannot be null or blank");
        }
    }

    /**
     * Returns the combined build fingerprint (model hash + DDL hash).
     *
     * <p>This fingerprint uniquely identifies the combination of:
     * <ul>
     *   <li>The exact SDD model structure</li>
     *   <li>The exact generated SQL</li>
     *   <li>The generator version</li>
     * </ul>
     *
     * @return SHA-256 hash of (model_hash + ddl_hash + version)
     */
    public String buildFingerprint() {
        return SdrHasher.computeHash(hash + SdrHasher.computeHash(ddl) + version);
    }
}
