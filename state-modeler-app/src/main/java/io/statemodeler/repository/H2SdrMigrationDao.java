package io.statemodeler.repository;

import io.vavr.control.Try;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object for SDR migration operations in H2 database.
 *
 * <p>Handles CRUD operations for migration scripts between SDR versions.
 */
final class H2SdrMigrationDao {

    private static final Logger logger = LoggerFactory.getLogger(H2SdrMigrationDao.class);

    private final H2ConnectionManager connectionManager;

    /**
     * Creates a new DAO with the specified connection manager.
     *
     * @param connectionManager the connection manager for database access
     * @throws IllegalArgumentException if connectionManager is null
     */
    H2SdrMigrationDao(H2ConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("connectionManager cannot be null");
        }
        this.connectionManager = connectionManager;
    }

    /**
     * Saves a migration script between two SDR versions.
     *
     * @param migration the migration record to persist
     * @return Success if saved, Failure if error occurs (e.g., duplicate, invalid refs)
     */
    Try<Void> saveMigration(SdrMigration migration) {
        if (migration == null) {
            return Try.failure(new IllegalArgumentException("migration cannot be null"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                    INSERT INTO sdr_migrations (from_hash, to_hash, migration_script, confidence, comments, dialect, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, migration.fromHash());
                stmt.setString(2, migration.toHash());
                stmt.setString(3, migration.migrationScript());
                stmt.setDouble(4, migration.confidence());
                stmt.setString(5, migration.comments());
                stmt.setString(6, migration.dialect());
                stmt.setTimestamp(7, Timestamp.from(migration.createdAt()));

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to insert migration");
                }
                logger.debug("Saved migration from {} to {}", migration.fromHash(), migration.toHash());
                return null;
            }
        });
    }

    /**
     * Retrieves a migration between two specific SDR hashes.
     *
     * @param fromHash source SDR hash
     * @param toHash target SDR hash
     * @return Success with Optional migration, Failure if database error
     */
    Try<Optional<SdrMigration>> findMigration(String fromHash, String toHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, confidence, comments, dialect, created_at
                    FROM sdr_migrations
                    WHERE from_hash = ? AND to_hash = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fromHash);
                stmt.setString(2, toHash);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(extractMigration(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    /**
     * Lists all migrations from a specific SDR hash.
     *
     * @param fromHash source SDR hash
     * @return Success with list of migrations, Failure if database error
     */
    Try<List<SdrMigration>> findMigrationsFrom(String fromHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, confidence, comments, dialect, created_at
                    FROM sdr_migrations
                    WHERE from_hash = ?
                    ORDER BY created_at DESC
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fromHash);
                return extractMigrationList(stmt);
            }
        });
    }

    /**
     * Lists all migrations to a specific SDR hash.
     *
     * @param toHash target SDR hash
     * @return Success with list of migrations, Failure if database error
     */
    Try<List<SdrMigration>> findMigrationsTo(String toHash) {
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, confidence, comments, dialect, created_at
                    FROM sdr_migrations
                    WHERE to_hash = ?
                    ORDER BY created_at DESC
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, toHash);
                return extractMigrationList(stmt);
            }
        });
    }

    /**
     * Deletes a migration between two SDR hashes.
     *
     * @param fromHash source SDR hash
     * @param toHash target SDR hash
     * @return Success with true if deleted, false if not found, Failure if database error
     */
    Try<Boolean> deleteMigration(String fromHash, String toHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(connectionManager::getConnection).of(conn -> {
            String sql = "DELETE FROM sdr_migrations WHERE from_hash = ? AND to_hash = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fromHash);
                stmt.setString(2, toHash);

                int rowsAffected = stmt.executeUpdate();
                boolean deleted = rowsAffected > 0;
                if (deleted) {
                    logger.debug("Deleted migration from {} to {}", fromHash, toHash);
                }
                return deleted;
            }
        });
    }

    /**
     * Extracts a single SdrMigration from a ResultSet (current row).
     *
     * @param rs the result set positioned at the migration row
     * @return the extracted SdrMigration
     * @throws SQLException if database error occurs
     */
    private SdrMigration extractMigration(ResultSet rs) throws SQLException {
        return new SdrMigration(
                rs.getString("from_hash"),
                rs.getString("to_hash"),
                rs.getString("migration_script"),
                rs.getDouble("confidence"),
                rs.getString("comments"),
                rs.getString("dialect"),
                rs.getTimestamp("created_at").toInstant());
    }

    /**
     * Extracts migration list from a prepared statement result set.
     *
     * @param stmt the prepared statement to execute
     * @return list of SdrMigration records
     * @throws SQLException if database error occurs
     */
    private List<SdrMigration> extractMigrationList(PreparedStatement stmt) throws SQLException {
        var results = new ArrayList<SdrMigration>();

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(extractMigration(rs));
            }
        }

        return results;
    }
}
