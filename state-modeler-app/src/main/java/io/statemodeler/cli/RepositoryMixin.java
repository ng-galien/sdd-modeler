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
     * Creates a new {@link H2SdrRepository} instance using the configured path.
     *
     * <p>The repository is {@link AutoCloseable} and should be used in try-with-resources.
     *
     * @return a new H2SdrRepository instance
     */
    public H2SdrRepository createRepository() {
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
}
