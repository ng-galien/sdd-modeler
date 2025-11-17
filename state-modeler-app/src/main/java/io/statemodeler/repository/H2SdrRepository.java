package io.statemodeler.repository;

import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H2 database implementation of {@link SdrRepository}.
 *
 * <p>Delegates to specialized DAOs for SDR records and migrations. Uses an embedded H2 database
 * for persistence with AUTO_SERVER enabled for multi-process access.
 *
 * <p>Thread-safe: Connection management is handled by H2's built-in mechanisms.
 */
public class H2SdrRepository implements SdrRepository {

    private static final Logger logger = LoggerFactory.getLogger(H2SdrRepository.class);

    private final H2ConnectionManager connectionManager;
    private final H2SdrRecordDao recordDao;
    private final H2SdrMigrationDao migrationDao;

    /**
     * Creates a new H2 SDR repository at the specified path.
     *
     * @param dbPath path to the database file (without .h2.db extension)
     */
    public H2SdrRepository(Path dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath cannot be null");
        }
        this.connectionManager = new H2ConnectionManager(dbPath);
        this.recordDao = new H2SdrRecordDao(connectionManager);
        this.migrationDao = new H2SdrMigrationDao(connectionManager);
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
        this.connectionManager = new H2ConnectionManager(jdbcUrl);
        this.recordDao = new H2SdrRecordDao(connectionManager);
        this.migrationDao = new H2SdrMigrationDao(connectionManager);
    }

    /**
     * Creates an in-memory H2 repository for testing purposes.
     *
     * <p>The database is ephemeral and will be lost when the connection is closed. This is much
     * faster than file-based databases and ideal for unit tests.
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

    // ========== SDR Record Operations (delegate to recordDao) ==========

    @Override
    public Try<Void> save(SdrRecord sdr, String modelName, String modelVersion) {
        return recordDao.save(sdr, modelName, modelVersion);
    }

    @Override
    public Try<Optional<SdrRecord>> findByHash(String schemaHash) {
        return recordDao.findByHash(schemaHash);
    }

    @Override
    public Try<List<SdrMetadata>> findByName(String modelName) {
        return recordDao.findByName(modelName);
    }

    @Override
    public Try<Optional<SdrRecord>> findByNameAndVersion(String modelName, String modelVersion) {
        return recordDao.findByNameAndVersion(modelName, modelVersion);
    }

    @Override
    public Try<List<SdrMetadata>> listAll() {
        return recordDao.listAll();
    }

    @Override
    public Try<List<SdrMetadata>> findRecent(int limit) {
        return recordDao.findRecent(limit);
    }

    @Override
    public Try<Boolean> delete(String schemaHash) {
        return recordDao.delete(schemaHash);
    }

    @Override
    public Try<Long> count() {
        return recordDao.count();
    }

    @Override
    public Try<Boolean> exists(String schemaHash) {
        return recordDao.exists(schemaHash);
    }

    // ========== Migration Operations (delegate to migrationDao) ==========

    @Override
    public Try<Void> saveMigration(SdrMigration migration) {
        return migrationDao.saveMigration(migration);
    }

    @Override
    public Try<Optional<SdrMigration>> findMigration(String fromHash, String toHash) {
        return migrationDao.findMigration(fromHash, toHash);
    }

    @Override
    public Try<List<SdrMigration>> findMigrationsFrom(String fromHash) {
        return migrationDao.findMigrationsFrom(fromHash);
    }

    @Override
    public Try<List<SdrMigration>> findMigrationsTo(String toHash) {
        return migrationDao.findMigrationsTo(toHash);
    }

    @Override
    public Try<Boolean> deleteMigration(String fromHash, String toHash) {
        return migrationDao.deleteMigration(fromHash, toHash);
    }

    // ========== Lifecycle ==========

    @Override
    public void close() {
        // H2 auto-closes connections, but we can trigger SHUTDOWN if needed
        Try.withResources(connectionManager::getConnection)
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
