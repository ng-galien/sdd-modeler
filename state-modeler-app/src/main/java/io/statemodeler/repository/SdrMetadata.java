package io.statemodeler.repository;

import java.time.Instant;

/**
 * Lightweight metadata for SDR listing without loading full CLOB content.
 *
 * <p>Used for displaying repository contents efficiently when full schema/DDL is not needed.
 *
 * @param schemaHash SHA-256 hash of the canonical schema (primary key)
 * @param modelName name of the SDD model (extracted from schema)
 * @param modelVersion version of the SDD model
 * @param sdrVersion version of the SDR factory that created this record
 * @param buildFingerprint combined hash of schema + DDL + SDR version
 * @param createdAt timestamp when this SDR was persisted
 */
public record SdrMetadata(
        String schemaHash,
        String modelName,
        String modelVersion,
        String sdrVersion,
        String buildFingerprint,
        Instant createdAt) {

    /**
     * Creates a new SdrMetadata with validation.
     *
     * @throws IllegalArgumentException if any field is null or hash/name/version are blank
     */
    public SdrMetadata {
        if (schemaHash == null || schemaHash.isBlank()) {
            throw new IllegalArgumentException("schemaHash cannot be null or blank");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName cannot be null or blank");
        }
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException("modelVersion cannot be null or blank");
        }
        if (sdrVersion == null || sdrVersion.isBlank()) {
            throw new IllegalArgumentException("sdrVersion cannot be null or blank");
        }
        if (buildFingerprint == null || buildFingerprint.isBlank()) {
            throw new IllegalArgumentException("buildFingerprint cannot be null or blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }

    /**
     * Returns the short hash (first 8 characters) for display purposes.
     *
     * @return truncated hash for human-readable output
     */
    public String shortHash() {
        return schemaHash.length() >= 8 ? schemaHash.substring(0, 8) : schemaHash;
    }
}
