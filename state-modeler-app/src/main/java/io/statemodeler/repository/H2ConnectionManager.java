package io.statemodeler.repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages H2 database connections and schema initialization for SDR repository.
 *
 * <p>Handles connection pooling via H2's built-in AUTO_SERVER mechanism and ensures schema tables
 * are created on initialization.
 *
 * <p>Thread-safe: Connection creation is delegated to H2's DriverManager which handles
 * synchronization.
 */
final class H2ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(H2ConnectionManager.class);

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
                confidence DOUBLE NOT NULL,
                comments CLOB NOT NULL,
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
     * Creates a connection manager for a file-based H2 database.
     *
     * @param dbPath path to the database file (without .h2.db extension)
     * @throws IllegalArgumentException if dbPath is null
     */
    H2ConnectionManager(Path dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath cannot be null");
        }
        this.jdbcUrl = "jdbc:h2:file:" + dbPath.toAbsolutePath() + ";AUTO_SERVER=TRUE";
        initializeSchema();
    }

    /**
     * Creates a connection manager with a custom JDBC URL.
     *
     * <p>Useful for testing with in-memory databases or custom configurations.
     *
     * @param jdbcUrl custom JDBC URL (e.g., "jdbc:h2:mem:testdb" for in-memory)
     * @throws IllegalArgumentException if jdbcUrl is null or blank
     */
    H2ConnectionManager(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl cannot be null or blank");
        }
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    /**
     * Gets a new connection to the H2 database.
     *
     * <p>Caller is responsible for closing the connection (use try-with-resources).
     *
     * @return a new database connection
     * @throws SQLException if connection fails
     */
    Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Returns the JDBC URL for this connection manager.
     *
     * @return the JDBC URL
     */
    String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Initializes the database schema (tables and indexes).
     *
     * <p>Safe to call multiple times - uses CREATE TABLE IF NOT EXISTS.
     *
     * @throws IllegalStateException if schema initialization fails
     */
    private void initializeSchema() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(SCHEMA_DDL);
            logger.info("SDR repository schema initialized at {}", jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize H2 schema at " + jdbcUrl, e);
        }
    }
}
