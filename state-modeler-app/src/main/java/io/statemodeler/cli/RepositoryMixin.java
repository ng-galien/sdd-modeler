package io.statemodeler.cli;

import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.repository.RepositoryConfig;
import io.statemodeler.repository.SdrMetadata;
import io.statemodeler.repository.SdrMigration;
import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Option;

/**
 * Reusable Picocli mixin for repository path configuration.
 *
 * <p>Inject this mixin into any command that needs access to the SDR repository:
 *
 * <pre>{@code
 * @Command(name = "mycommand")
 * class MyCommand implements Callable<Integer> {
 *     @Mixin
 *     RepositoryMixin repositoryMixin;
 *
 *     @Override
 *     public Integer call() {
 *         try (var repo = repositoryMixin.createRepository()) {
 *             // use repository...
 *         }
 *         return 0;
 *     }
 * }
 * }</pre>
 */
public class RepositoryMixin {

    @Option(
            names = {"--repository", "-r"},
            description =
                    "Path to SDR repository database (default: ~/.sdd-modeler/repository, env: SDD_REPOSITORY_PATH)",
            paramLabel = "<path>")
    String repositoryPath;

    /**
     * Pre-created repository for testing purposes.
     *
     * <p>When set, {@link #createRepository()} returns a non-closing wrapper around this instance
     * instead of creating a new one. This allows tests to inject in-memory repositories without
     * them being closed by command try-with-resources blocks.
     */
    H2SdrRepository testRepository;

    /**
     * Creates a new {@link SdrRepository} instance using the configured path.
     *
     * <p>The repository is {@link AutoCloseable} and should be used in try-with-resources.
     *
     * <p>For testing: if {@code testRepository} is set, returns a non-closing wrapper around that
     * instance to prevent test repositories from being closed prematurely.
     *
     * @return a new SdrRepository instance (H2SdrRepository or wrapper for tests)
     */
    public SdrRepository createRepository() {
        if (testRepository != null) {
            return new NonClosingRepositoryWrapper(testRepository);
        }
        return RepositoryConfig.createRepository(repositoryPath);
    }

    /**
     * Gets the configured repository path (for testing/debugging).
     *
     * @return the repository path option (may be null if not specified)
     */
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Non-closing wrapper for H2SdrRepository used in tests.
     *
     * <p>Implements {@link SdrRepository} directly using pure delegation pattern without extending
     * H2SdrRepository. This avoids creating unnecessary database connections in the constructor.
     *
     * <p>Delegates all method calls to the wrapped repository but prevents {@link #close()} from
     * being called. This allows test repositories to be reused across multiple command invocations
     * without being prematurely closed by try-with-resources blocks.
     */
    private static class NonClosingRepositoryWrapper implements SdrRepository {
        private final H2SdrRepository delegate;

        NonClosingRepositoryWrapper(H2SdrRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Try<Void> save(SdrRecord record, String name, String version) {
            return delegate.save(record, name, version);
        }

        @Override
        public Try<Optional<SdrRecord>> findByHash(String hash) {
            return delegate.findByHash(hash);
        }

        @Override
        public Try<List<SdrMetadata>> findByName(String modelName) {
            return delegate.findByName(modelName);
        }

        @Override
        public Try<Optional<SdrRecord>> findByNameAndVersion(String name, String version) {
            return delegate.findByNameAndVersion(name, version);
        }

        @Override
        public Try<List<SdrMetadata>> listAll() {
            return delegate.listAll();
        }

        @Override
        public Try<List<SdrMetadata>> findRecent(int limit) {
            return delegate.findRecent(limit);
        }

        @Override
        public Try<Boolean> delete(String hash) {
            return delegate.delete(hash);
        }

        @Override
        public Try<Long> count() {
            return delegate.count();
        }

        @Override
        public Try<Boolean> exists(String hash) {
            return delegate.exists(hash);
        }

        @Override
        public Try<Void> saveMigration(SdrMigration migration) {
            return delegate.saveMigration(migration);
        }

        @Override
        public Try<Optional<SdrMigration>> findMigration(String fromHash, String toHash) {
            return delegate.findMigration(fromHash, toHash);
        }

        @Override
        public Try<List<SdrMigration>> findMigrationsFrom(String fromHash) {
            return delegate.findMigrationsFrom(fromHash);
        }

        @Override
        public Try<List<SdrMigration>> findMigrationsTo(String toHash) {
            return delegate.findMigrationsTo(toHash);
        }

        @Override
        public Try<Boolean> deleteMigration(String fromHash, String toHash) {
            return delegate.deleteMigration(fromHash, toHash);
        }

        @Override
        public void close() {
            // Do NOT close the delegate repository - it's owned by the test
        }
    }
}
