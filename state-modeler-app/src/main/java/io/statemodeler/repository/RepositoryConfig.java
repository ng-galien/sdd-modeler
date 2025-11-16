package io.statemodeler.repository;

import java.nio.file.Path;

/**
 * Configuration utility for resolving the SDR repository path.
 *
 * <p>Resolution order (highest priority first):
 *
 * <ol>
 *   <li>CLI option ({@code --repository <path>})
 *   <li>Environment variable ({@code SDD_REPOSITORY_PATH})
 *   <li>Config file ({@code ~/.sdd-modeler/config.yaml}) - future enhancement
 *   <li>Default path ({@code ~/.sdd-modeler/repository})
 * </ol>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Path repoPath = RepositoryConfig.resolveRepositoryPath(cliOption);
 * try (var repo = new H2SdrRepository(repoPath)) {
 *     // use repository...
 * }
 * }</pre>
 */
public final class RepositoryConfig {

    /** Default repository location: {@code ~/.sdd-modeler/repository} */
    public static final Path DEFAULT_REPOSITORY_PATH =
            Path.of(System.getProperty("user.home"), ".sdd-modeler", "repository");

    /** Environment variable name for repository path override */
    public static final String ENV_REPOSITORY_PATH = "SDD_REPOSITORY_PATH";

    private RepositoryConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves the repository path using the priority cascade.
     *
     * @param cliOption the path provided via CLI {@code --repository} option (may be null)
     * @return the resolved repository path (never null)
     */
    public static Path resolveRepositoryPath(String cliOption) {
        // 1. CLI option has highest priority
        if (cliOption != null && !cliOption.isBlank()) {
            return Path.of(cliOption);
        }

        // 2. Environment variable
        String envPath = System.getenv(ENV_REPOSITORY_PATH);
        if (envPath != null && !envPath.isBlank()) {
            return Path.of(envPath);
        }

        // 3. Config file (future enhancement)
        // Path configPath = readFromConfigFile();
        // if (configPath != null) {
        //     return configPath;
        // }

        // 4. Default fallback
        return DEFAULT_REPOSITORY_PATH;
    }

    /**
     * Creates a new {@link H2SdrRepository} instance using the resolved path.
     *
     * @param cliOption the path provided via CLI {@code --repository} option (may be null)
     * @return a new H2SdrRepository instance
     */
    public static H2SdrRepository createRepository(String cliOption) {
        Path path = resolveRepositoryPath(cliOption);
        return new H2SdrRepository(path);
    }
}
