package io.statemodeler.repository;

import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;

/**
 * Repository for persisting and retrieving State Definition Records (SDR).
 *
 * <p>Provides CRUD operations for managing versioned SDD models with their generated artifacts.
 * Uses Vavr's {@link Try} for functional error handling.
 *
 * <p>Implementation note: The schema hash serves as the primary key, ensuring each unique
 * model structure is stored only once. Multiple versions of a model (with different names/versions
 * but identical structure) will share the same schema hash.
 *
 * <p>Implementations should be {@link AutoCloseable} to support try-with-resources.
 */
public interface SdrRepository extends AutoCloseable {

    /**
     * Persists an SDR in the repository.
     *
     * <p>The model name and version are extracted from the schema JSON or can be explicitly
     * provided. If an SDR with the same schema hash already exists, returns a Failure.
     *
     * @param sdr the SDR record to persist (non-null)
     * @param modelName name of the SDD model (non-null, non-blank)
     * @param modelVersion version of the SDD model (non-null, non-blank)
     * @return Success if saved, Failure if error occurs (e.g., duplicate hash)
     */
    Try<Void> save(SdrRecord sdr, String modelName, String modelVersion);

    /**
     * Retrieves an SDR by its schema hash.
     *
     * @param schemaHash SHA-256 hash of the canonical schema
     * @return Success with Optional SDR if operation succeeds, Failure if database error
     */
    Try<Optional<SdrRecord>> findByHash(String schemaHash);

    /**
     * Lists all versions of a model by name.
     *
     * <p>Results are sorted by creation date in descending order (most recent first).
     *
     * @param modelName name of the model to search for
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> findByName(String modelName);

    /**
     * Retrieves an SDR by exact model name and version.
     *
     * <p>If multiple SDRs exist with the same name and version (edge case),
     * returns the most recently created one.
     *
     * @param modelName name of the model
     * @param modelVersion version of the model
     * @return Success with Optional SDR if operation succeeds, Failure if database error
     */
    Try<Optional<SdrRecord>> findByNameAndVersion(String modelName, String modelVersion);

    /**
     * Lists all SDRs in the repository (metadata only).
     *
     * <p>Returns lightweight metadata without loading full schema/DDL CLOBs.
     * Sorted by creation date in descending order.
     *
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> listAll();

    /**
     * Lists the N most recently created SDRs.
     *
     * @param limit maximum number of results to return
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> findRecent(int limit);

    /**
     * Deletes an SDR from the repository.
     *
     * @param schemaHash hash of the SDR to delete
     * @return Success with true if deleted, false if not found, Failure if database error
     */
    Try<Boolean> delete(String schemaHash);

    /**
     * Counts the total number of SDRs in the repository.
     *
     * @return Success with count, Failure if database error
     */
    Try<Long> count();

    /**
     * Checks if an SDR exists with the given schema hash.
     *
     * @param schemaHash hash to check
     * @return Success with true/false, Failure if database error
     */
    Try<Boolean> exists(String schemaHash);

    // ========== Migration Management ==========

    /**
     * Saves a migration script between two SDR versions.
     *
     * @param migration the migration record to persist
     * @return Success if saved, Failure if error occurs (e.g., duplicate, invalid refs)
     */
    Try<Void> saveMigration(SdrMigration migration);

    /**
     * Retrieves a migration between two specific SDR hashes.
     *
     * @param fromHash source SDR hash
     * @param toHash target SDR hash
     * @return Success with Optional migration, Failure if database error
     */
    Try<Optional<SdrMigration>> findMigration(String fromHash, String toHash);

    /**
     * Lists all migrations from a specific SDR hash.
     *
     * @param fromHash source SDR hash
     * @return Success with list of migrations, Failure if database error
     */
    Try<List<SdrMigration>> findMigrationsFrom(String fromHash);

    /**
     * Lists all migrations to a specific SDR hash.
     *
     * @param toHash target SDR hash
     * @return Success with list of migrations, Failure if database error
     */
    Try<List<SdrMigration>> findMigrationsTo(String toHash);

    /**
     * Deletes a migration between two SDR hashes.
     *
     * @param fromHash source SDR hash
     * @param toHash target SDR hash
     * @return Success with true if deleted, false if not found, Failure if database error
     */
    Try<Boolean> deleteMigration(String fromHash, String toHash);
}
