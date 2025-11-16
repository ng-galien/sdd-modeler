package io.statemodeler.cli;

import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.repository.RepositoryConfig;
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
     * Creates a new {@link H2SdrRepository} instance using the configured path.
     *
     * <p>The repository is {@link AutoCloseable} and should be used in try-with-resources.
     *
     * <p>For testing: if {@code testRepository} is set, returns a non-closing wrapper around that
     * instance to prevent test repositories from being closed prematurely.
     *
     * @return a new H2SdrRepository instance, or a wrapper around the injected test repository
     */
    public H2SdrRepository createRepository() {
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
     * <p>Delegates all method calls to the wrapped repository but prevents {@link #close()} from
     * being called. This allows test repositories to be reused across multiple command invocations
     * without being prematurely closed by try-with-resources blocks.
     */
    private static class NonClosingRepositoryWrapper extends H2SdrRepository {
        private final H2SdrRepository delegate;

        NonClosingRepositoryWrapper(H2SdrRepository delegate) {
            super("jdbc:h2:mem:wrapper"); // Dummy connection, never used
            this.delegate = delegate;
        }

        @Override
        public io.vavr.control.Try<Void> save(io.statemodeler.sdr.SdrRecord record, String name, String version) {
            return delegate.save(record, name, version);
        }

        @Override
        public io.vavr.control.Try<java.util.Optional<io.statemodeler.sdr.SdrRecord>> findByHash(String hash) {
            return delegate.findByHash(hash);
        }

        @Override
        public io.vavr.control.Try<java.util.List<io.statemodeler.repository.SdrMetadata>> findByName(
                String modelName) {
            return delegate.findByName(modelName);
        }

        @Override
        public io.vavr.control.Try<java.util.Optional<io.statemodeler.sdr.SdrRecord>> findByNameAndVersion(
                String name, String version) {
            return delegate.findByNameAndVersion(name, version);
        }

        @Override
        public io.vavr.control.Try<java.util.List<io.statemodeler.repository.SdrMetadata>> listAll() {
            return delegate.listAll();
        }

        @Override
        public io.vavr.control.Try<java.util.List<io.statemodeler.repository.SdrMetadata>> findRecent(int limit) {
            return delegate.findRecent(limit);
        }

        @Override
        public io.vavr.control.Try<Boolean> delete(String hash) {
            return delegate.delete(hash);
        }

        @Override
        public io.vavr.control.Try<Long> count() {
            return delegate.count();
        }

        @Override
        public io.vavr.control.Try<Boolean> exists(String hash) {
            return delegate.exists(hash);
        }

        @Override
        public io.vavr.control.Try<Void> saveMigration(io.statemodeler.repository.SdrMigration migration) {
            return delegate.saveMigration(migration);
        }

        @Override
        public io.vavr.control.Try<java.util.Optional<io.statemodeler.repository.SdrMigration>> findMigration(
                String fromHash, String toHash) {
            return delegate.findMigration(fromHash, toHash);
        }

        @Override
        public io.vavr.control.Try<java.util.List<io.statemodeler.repository.SdrMigration>> findMigrationsFrom(
                String fromHash) {
            return delegate.findMigrationsFrom(fromHash);
        }

        @Override
        public io.vavr.control.Try<java.util.List<io.statemodeler.repository.SdrMigration>> findMigrationsTo(
                String toHash) {
            return delegate.findMigrationsTo(toHash);
        }

        @Override
        public io.vavr.control.Try<Boolean> deleteMigration(String fromHash, String toHash) {
            return delegate.deleteMigration(fromHash, toHash);
        }

        @Override
        public void close() {
            // Do NOT close the delegate repository - it's owned by the test
        }
    }
}
