package io.statemodeler.repository;

import io.statemodeler.sdr.SdrRecord;
import java.util.List;
import java.util.Optional;

/**
 * Repository for persisting and retrieving State Definition Records (SDR).
 *
 * <p>Provides CRUD operations for managing versioned SDD models with their generated artifacts.
 *
 * <p>Implementation note: The schema hash serves as the primary key, ensuring each unique
 * model structure is stored only once. Multiple versions of a model (with different names/versions
 * but identical structure) will share the same schema hash.
 */
public interface SdrRepository {

    /**
     * Persists an SDR in the repository.
     *
     * <p>The model name and version are extracted from the schema JSON or can be explicitly
     * provided. If an SDR with the same schema hash already exists, this method will throw.
     *
     * @param sdr the SDR record to persist (non-null)
     * @param modelName name of the SDD model (non-null, non-blank)
     * @param modelVersion version of the SDD model (non-null, non-blank)
     * @throws IllegalArgumentException if an SDR with this schema hash already exists
     * @throws IllegalArgumentException if parameters are null/blank
     */
    void save(SdrRecord sdr, String modelName, String modelVersion);

    /**
     * Retrieves an SDR by its schema hash.
     *
     * @param schemaHash SHA-256 hash of the canonical schema
     * @return the SDR if found, empty otherwise
     */
    Optional<SdrRecord> findByHash(String schemaHash);

    /**
     * Lists all versions of a model by name.
     *
     * <p>Results are sorted by creation date in descending order (most recent first).
     *
     * @param modelName name of the model to search for
     * @return list of metadata for all matching SDRs (empty if none found)
     */
    List<SdrMetadata> findByName(String modelName);

    /**
     * Retrieves an SDR by exact model name and version.
     *
     * <p>If multiple SDRs exist with the same name and version (edge case),
     * returns the most recently created one.
     *
     * @param modelName name of the model
     * @param modelVersion version of the model
     * @return the SDR if found, empty otherwise
     */
    Optional<SdrRecord> findByNameAndVersion(String modelName, String modelVersion);

    /**
     * Lists all SDRs in the repository (metadata only).
     *
     * <p>Returns lightweight metadata without loading full schema/DDL CLOBs.
     * Sorted by creation date in descending order.
     *
     * @return list of all SDR metadata (empty if repository is empty)
     */
    List<SdrMetadata> listAll();

    /**
     * Lists the N most recently created SDRs.
     *
     * @param limit maximum number of results to return
     * @return list of recent SDR metadata (empty if repository is empty)
     */
    List<SdrMetadata> findRecent(int limit);

    /**
     * Deletes an SDR from the repository.
     *
     * @param schemaHash hash of the SDR to delete
     * @return true if deleted, false if not found
     */
    boolean delete(String schemaHash);

    /**
     * Counts the total number of SDRs in the repository.
     *
     * @return total count (0 if empty)
     */
    long count();

    /**
     * Checks if an SDR exists with the given schema hash.
     *
     * @param schemaHash hash to check
     * @return true if exists, false otherwise
     */
    boolean exists(String schemaHash);
}
