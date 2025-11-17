package io.statemodeler.repository;

import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object for SDR record operations in H2 database.
 *
 * <p>Handles CRUD operations for {@link SdrRecord} persistence, excluding migration-related
 * operations which are handled by {@link H2SdrMigrationDao}.
 */
final class H2SdrRecordDao {

    private static final Logger logger = LoggerFactory.getLogger(H2SdrRecordDao.class);

    private final H2ConnectionManager connectionManager;

    /**
     * Creates a new DAO with the specified connection manager.
     *
     * @param connectionManager the connection manager for database access
     * @throws IllegalArgumentException if connectionManager is null
     */
    H2SdrRecordDao(H2ConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("connectionManager cannot be null");
        }
        this.connectionManager = connectionManager;
    }

    /**
     * Persists an SDR in the repository.
     *
     * @param sdr the SDR record to persist (non-null)
     * @param modelName name of the SDD model (non-null, non-blank)
     * @param modelVersion version of the SDD model (non-null, non-blank)
     * @return Success if saved, Failure if error occurs (e.g., duplicate hash)
     */
    Try<Void> save(SdrRecord sdr, String modelName, String modelVersion) {
        if (sdr == null) {
            return Try.failure(new IllegalArgumentException("sdr cannot be null"));
        }
        if (modelName == null || modelName.isBlank()) {
            return Try.failure(new IllegalArgumentException("modelName cannot be null or blank"));
        }
        if (modelVersion == null || modelVersion.isBlank()) {
            return Try.failure(new IllegalArgumentException("modelVersion cannot be null or blank"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            INSERT INTO sdr_records
                            (schema_hash, model_name, model_version, schema_json, content_type,
                             ddl_sql, ddl_hash, sdr_version, build_fingerprint, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sdr.schemaHash());
                stmt.setString(2, modelName);
                stmt.setString(3, modelVersion);
                stmt.setString(4, sdr.schema());
                stmt.setString(5, sdr.contentType());
                stmt.setString(6, sdr.ddl());
                stmt.setString(7, sdr.ddlHash());
                stmt.setString(8, sdr.version());
                stmt.setString(9, sdr.buildFingerprint());
                stmt.setTimestamp(10, Timestamp.from(Instant.now()));

                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Failed to insert SDR");
                }

                String hashPreview =
                        sdr.schemaHash().length() > 8 ? sdr.schemaHash().substring(0, 8) : sdr.schemaHash();
                logger.info("Saved SDR: {} ({}:{})", hashPreview, modelName, modelVersion);
                return (Void) null;
            } catch (SQLException e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("PRIMARY KEY") || msg.contains("Unique"))) {
                    throw new IllegalArgumentException("SDR with hash " + sdr.schemaHash() + " already exists", e);
                }
                throw e;
            }
        });
    }

    /**
     * Retrieves an SDR by its schema hash.
     *
     * @param schemaHash SHA-256 hash of the canonical schema
     * @return Success with Optional SDR if operation succeeds, Failure if database error
     */
    Try<Optional<SdrRecord>> findByHash(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(Optional.empty());
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_json, content_type, ddl_sql, ddl_hash, sdr_version
                            FROM sdr_records
                            WHERE schema_hash = ?
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, schemaHash);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        var sdr = new SdrRecord(
                                rs.getString("schema_json"),
                                rs.getString("content_type"),
                                rs.getString("ddl_sql"),
                                schemaHash,
                                rs.getString("ddl_hash"),
                                rs.getString("sdr_version"));
                        return Optional.of(sdr);
                    }
                    return Optional.empty();
                }
            }
        });
    }

    /**
     * Lists all versions of a model by name.
     *
     * <p>Results are sorted by creation date in descending order (most recent first).
     *
     * @param modelName name of the model to search for
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> findByName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return Try.success(List.of());
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version, build_fingerprint, created_at
                            FROM sdr_records
                            WHERE model_name = ?
                            ORDER BY created_at DESC
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, modelName);
                return extractMetadataList(stmt);
            }
        });
    }

    /**
     * Retrieves an SDR by exact model name and version.
     *
     * <p>If multiple SDRs exist with the same name and version (edge case), returns the most
     * recently created one.
     *
     * @param modelName name of the model
     * @param modelVersion version of the model
     * @return Success with Optional SDR if operation succeeds, Failure if database error
     */
    Try<Optional<SdrRecord>> findByNameAndVersion(String modelName, String modelVersion) {
        if (modelName == null || modelName.isBlank() || modelVersion == null || modelVersion.isBlank()) {
            return Try.success(Optional.empty());
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, schema_json, content_type, ddl_sql, ddl_hash, sdr_version
                            FROM sdr_records
                            WHERE model_name = ? AND model_version = ?
                            ORDER BY created_at DESC
                            LIMIT 1
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, modelName);
                stmt.setString(2, modelVersion);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        var sdr = new SdrRecord(
                                rs.getString("schema_json"),
                                rs.getString("content_type"),
                                rs.getString("ddl_sql"),
                                rs.getString("schema_hash"),
                                rs.getString("ddl_hash"),
                                rs.getString("sdr_version"));
                        return Optional.of(sdr);
                    }
                    return Optional.empty();
                }
            }
        });
    }

    /**
     * Lists all SDRs in the repository (metadata only).
     *
     * <p>Returns lightweight metadata without loading full schema/DDL CLOBs. Sorted by creation
     * date in descending order.
     *
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> listAll() {
        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version, build_fingerprint, created_at
                            FROM sdr_records
                            ORDER BY created_at DESC
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                return extractMetadataList(stmt);
            }
        });
    }

    /**
     * Lists the N most recently created SDRs.
     *
     * @param limit maximum number of results to return
     * @return Success with list of metadata, Failure if database error
     */
    Try<List<SdrMetadata>> findRecent(int limit) {
        if (limit <= 0) {
            return Try.success(List.of());
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version, build_fingerprint, created_at
                            FROM sdr_records
                            ORDER BY created_at DESC
                            LIMIT ?
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                return extractMetadataList(stmt);
            }
        });
    }

    /**
     * Deletes an SDR from the repository.
     *
     * @param schemaHash hash of the SDR to delete
     * @return Success with true if deleted, false if not found, Failure if database error
     */
    Try<Boolean> delete(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(false);
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = "DELETE FROM sdr_records WHERE schema_hash = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, schemaHash);
                int rows = stmt.executeUpdate();
                boolean deleted = rows > 0;

                if (deleted) {
                    String hashPreview = schemaHash.length() > 8 ? schemaHash.substring(0, 8) : schemaHash;
                    logger.info("Deleted SDR: {}", hashPreview);
                }
                return deleted;
            }
        });
    }

    /**
     * Counts the total number of SDRs in the repository.
     *
     * @return Success with count, Failure if database error
     */
    Try<Long> count() {
        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = "SELECT COUNT(*) FROM sdr_records";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        });
    }

    /**
     * Checks if an SDR exists with the given schema hash.
     *
     * @param schemaHash hash to check
     * @return Success with true/false, Failure if database error
     */
    Try<Boolean> exists(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(false);
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = "SELECT 1 FROM sdr_records WHERE schema_hash = ? LIMIT 1";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, schemaHash);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /**
     * Extracts metadata list from a prepared statement result set.
     *
     * @param stmt the prepared statement to execute
     * @return list of SDR metadata records
     * @throws SQLException if database error occurs
     */
    private List<SdrMetadata> extractMetadataList(PreparedStatement stmt) throws SQLException {
        var results = new ArrayList<SdrMetadata>();

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                var metadata = new SdrMetadata(
                        rs.getString("schema_hash"),
                        rs.getString("model_name"),
                        rs.getString("model_version"),
                        rs.getString("sdr_version"),
                        rs.getString("build_fingerprint"),
                        rs.getTimestamp("created_at").toInstant());
                results.add(metadata);
            }
        }

        return results;
    }
}
