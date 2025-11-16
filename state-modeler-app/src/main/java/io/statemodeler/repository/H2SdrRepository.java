package io.statemodeler.repository;

import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H2 database implementation of {@link SdrRepository}.
 *
 * <p>Uses an embedded H2 database for persistence. Connection URL format:
 * {@code jdbc:h2:file:<path>;AUTO_SERVER=TRUE}
 *
 * <p>Thread-safe: Uses connection pooling via H2's built-in mechanisms.
 */
public class H2SdrRepository implements SdrRepository, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(H2SdrRepository.class);

    private static final String SCHEMA_DDL = """
            CREATE TABLE IF NOT EXISTS sdr_records (
                schema_hash VARCHAR(64) PRIMARY KEY,
                model_name VARCHAR(255) NOT NULL,
                model_version VARCHAR(50) NOT NULL,
                schema_json CLOB NOT NULL,
                content_type VARCHAR(100) NOT NULL,
                ddl_sql CLOB NOT NULL,
                ddl_hash VARCHAR(64) NOT NULL,
                sdr_version VARCHAR(20) NOT NULL,
                build_fingerprint VARCHAR(64) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_model_name ON sdr_records(model_name);
            CREATE INDEX IF NOT EXISTS idx_model_version ON sdr_records(model_name, model_version);
            CREATE INDEX IF NOT EXISTS idx_created_at ON sdr_records(created_at);

            CREATE TABLE IF NOT EXISTS sdr_migrations (
                from_hash VARCHAR(64) NOT NULL,
                to_hash VARCHAR(64) NOT NULL,
                migration_script CLOB NOT NULL,
                dialect VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (from_hash, to_hash),
                FOREIGN KEY (from_hash) REFERENCES sdr_records(schema_hash) ON DELETE CASCADE,
                FOREIGN KEY (to_hash) REFERENCES sdr_records(schema_hash) ON DELETE CASCADE
            );
            CREATE INDEX IF NOT EXISTS idx_migration_from ON sdr_migrations(from_hash);
            CREATE INDEX IF NOT EXISTS idx_migration_to ON sdr_migrations(to_hash);
            """;

    private final String jdbcUrl;

    /**
     * Creates a new H2 SDR repository at the specified path.
     *
     * @param dbPath path to the database file (without .h2.db extension)
     */
    public H2SdrRepository(Path dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath cannot be null");
        }
        this.jdbcUrl = "jdbc:h2:file:" + dbPath.toAbsolutePath() + ";AUTO_SERVER=TRUE";
        initializeSchema();
    }

    /**
     * Creates a new H2 SDR repository with a custom JDBC URL.
     *
     * <p>This constructor is useful for testing with in-memory databases or custom configurations.
     *
     * @param jdbcUrl custom JDBC URL (e.g., "jdbc:h2:mem:testdb" for in-memory)
     */
    public H2SdrRepository(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl cannot be null or blank");
        }
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    /**
     * Creates an in-memory H2 repository for testing purposes.
     *
     * <p>The database is ephemeral and will be lost when the connection is closed.
     * This is much faster than file-based databases and ideal for unit tests.
     *
     * @param dbName name of the in-memory database (used to isolate test databases)
     * @return a new H2SdrRepository backed by an in-memory database
     */
    public static H2SdrRepository createInMemory(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalArgumentException("dbName cannot be null or blank");
        }
        return new H2SdrRepository("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
    }

    /**
     * Creates schema and indexes if they don't exist.
     */
    private void initializeSchema() {
        Try.withResources(this::getConnection)
                .of(conn -> {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(SCHEMA_DDL);
                        logger.info("SDR repository schema initialized at {}", jdbcUrl);
                        return null;
                    }
                })
                .getOrElseThrow(e -> new IllegalStateException("Failed to initialize H2 schema", e));
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public Try<Void> save(SdrRecord sdr, String modelName, String modelVersion) {
        if (sdr == null) {
            return Try.failure(new IllegalArgumentException("sdr cannot be null"));
        }
        if (modelName == null || modelName.isBlank()) {
            return Try.failure(new IllegalArgumentException("modelName cannot be null or blank"));
        }
        if (modelVersion == null || modelVersion.isBlank()) {
            return Try.failure(new IllegalArgumentException("modelVersion cannot be null or blank"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
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

    @Override
    public Try<Optional<SdrRecord>> findByHash(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(Optional.empty());
        }

        return Try.withResources(this::getConnection).of(conn -> {
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

    @Override
    public Try<List<SdrMetadata>> findByName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return Try.success(List.of());
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version,
                                   build_fingerprint, created_at
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

    @Override
    public Try<Optional<SdrRecord>> findByNameAndVersion(String modelName, String modelVersion) {
        if (modelName == null || modelName.isBlank() || modelVersion == null || modelVersion.isBlank()) {
            return Try.success(Optional.empty());
        }

        return Try.withResources(this::getConnection).of(conn -> {
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

    @Override
    public Try<List<SdrMetadata>> listAll() {
        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version,
                                   build_fingerprint, created_at
                            FROM sdr_records
                            ORDER BY created_at DESC
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                return extractMetadataList(stmt);
            }
        });
    }

    @Override
    public Try<List<SdrMetadata>> findRecent(int limit) {
        if (limit <= 0) {
            return Try.success(List.of());
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                            SELECT schema_hash, model_name, model_version, sdr_version,
                                   build_fingerprint, created_at
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

    @Override
    public Try<Boolean> delete(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(false);
        }

        return Try.withResources(this::getConnection).of(conn -> {
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

    @Override
    public Try<Long> count() {
        return Try.withResources(this::getConnection).of(conn -> {
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

    @Override
    public Try<Boolean> exists(String schemaHash) {
        if (schemaHash == null || schemaHash.isBlank()) {
            return Try.success(false);
        }

        return Try.withResources(this::getConnection).of(conn -> {
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
     * Extracts metadata list from a prepared statement.
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

    // ========== Migration Management ==========

    @Override
    public Try<Void> saveMigration(SdrMigration migration) {
        if (migration == null) {
            return Try.failure(new IllegalArgumentException("migration cannot be null"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                    INSERT INTO sdr_migrations (from_hash, to_hash, migration_script, dialect, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, migration.fromHash());
                stmt.setString(2, migration.toHash());
                stmt.setString(3, migration.migrationScript());
                stmt.setString(4, migration.dialect());
                stmt.setTimestamp(5, Timestamp.from(migration.createdAt()));

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to insert migration");
                }
                logger.debug("Saved migration from {} to {}", migration.fromHash(), migration.toHash());
                return null;
            }
        });
    }

    @Override
    public Try<Optional<SdrMigration>> findMigration(String fromHash, String toHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, dialect, created_at
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

    @Override
    public Try<List<SdrMigration>> findMigrationsFrom(String fromHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, dialect, created_at
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

    @Override
    public Try<List<SdrMigration>> findMigrationsTo(String toHash) {
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
            String sql = """
                    SELECT from_hash, to_hash, migration_script, dialect, created_at
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

    @Override
    public Try<Boolean> deleteMigration(String fromHash, String toHash) {
        if (fromHash == null || fromHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("fromHash cannot be null or blank"));
        }
        if (toHash == null || toHash.isBlank()) {
            return Try.failure(new IllegalArgumentException("toHash cannot be null or blank"));
        }

        return Try.withResources(this::getConnection).of(conn -> {
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
     */
    private SdrMigration extractMigration(ResultSet rs) throws SQLException {
        return new SdrMigration(
                rs.getString("from_hash"),
                rs.getString("to_hash"),
                rs.getString("migration_script"),
                rs.getString("dialect"),
                rs.getTimestamp("created_at").toInstant());
    }

    /**
     * Extracts migration list from a prepared statement.
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

    @Override
    public void close() {
        // H2 auto-closes connections, but we can trigger SHUTDOWN if needed
        Try.withResources(this::getConnection)
                .of(conn -> {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SHUTDOWN");
                        logger.info("H2 repository closed");
                    }
                    return null;
                })
                .onFailure(e -> logger.warn("Error closing H2 repository: {}", e.getMessage()));
    }
}
