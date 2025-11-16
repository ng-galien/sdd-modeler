package io.statemodeler.sdr;

/**
 * Service Provider Interface for creating State Definition Records (SDR).
 *
 * <p>Implementations are discovered via Java SPI mechanism. To register a custom implementation:
 * <ol>
 *   <li>Implement this interface</li>
 *   <li>Create META-INF/services/io.statemodeler.sdr.SdrFactory file</li>
 *   <li>Add the fully-qualified class name to the file</li>
 * </ol>
 *
 * <p>The default implementation ({@link DefaultSdrFactory}) is always available.
 */
public interface SdrFactory {

    /**
     * Creates an SDR from a model source and SQL dialect.
     *
     * <p>The factory will:
     * <ul>
     *   <li>Parse the input (YAML or JSON) into an SDD model</li>
     *   <li>Normalize the model to canonical JSON format</li>
     *   <li>Generate DDL for the specified dialect</li>
     *   <li>Compute stable cryptographic hashes</li>
     * </ul>
     *
     * @param modelSource the SDD model as YAML or JSON string (non-null, non-empty)
     * @param contentType the content type: "application/yaml" or "application/json" (non-null)
     * @param sqlDialect the SQL dialect for DDL generation (e.g., "postgres")
     * @return an immutable SdrRecord with schema, DDL, and hash
     * @throws IllegalArgumentException if parameters are invalid or parsing fails
     */
    SdrRecord create(String modelSource, String contentType, String sqlDialect);

    /**
     * Returns the version identifier for this SDR factory implementation.
     *
     * <p>Version format: "major.minor.patch" (e.g., "1.0.0")
     *
     * @return the version string
     */
    String version();
}
