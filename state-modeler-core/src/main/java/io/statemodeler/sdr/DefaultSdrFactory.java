package io.statemodeler.sdr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.statemodeler.core.SddModel;
import io.statemodeler.dsl.JsonModelLoader;
import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.vavr.control.Try;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link SdrFactory}.
 *
 * <p>Provides stable, deterministic SDR creation with:
 * <ul>
 *   <li>Format-independent model parsing (YAML/JSON)</li>
 *   <li>Canonical JSON serialization for hashing</li>
 *   <li>SQL DDL generation for specified dialect</li>
 *   <li>Cryptographic hash computation (SHA-256)</li>
 * </ul>
 *
 * <p>Thread-safe and stateless.
 */
public class DefaultSdrFactory implements SdrFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSdrFactory.class);
    private static final String VERSION = "1.0.0";

    private final YamlModelLoader yamlLoader;
    private final JsonModelLoader jsonLoader;
    private final ObjectMapper canonicalMapper;

    /**
     * Creates a new DefaultSdrFactory with default configuration.
     */
    public DefaultSdrFactory() {
        this.yamlLoader = new YamlModelLoader();
        this.jsonLoader = new JsonModelLoader();
        this.canonicalMapper = createCanonicalMapper();
    }

    @Override
    public SdrRecord create(String modelSource, String contentType, String sqlDialect) {
        if (modelSource == null || modelSource.isBlank()) {
            throw new IllegalArgumentException("modelSource cannot be null or blank");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType cannot be null or blank");
        }
        if (sqlDialect == null || sqlDialect.isBlank()) {
            throw new IllegalArgumentException("sqlDialect cannot be null or blank");
        }

        logger.debug("Creating SDR for contentType={}, dialect={}", contentType, sqlDialect);

        // Parse model based on content type
        SddModel model = parseModel(modelSource, contentType);

        // Serialize to canonical JSON (deterministic ordering)
        String canonicalJson = serializeToCanonicalJson(model);

        // Compute stable hash of canonical representation
        String modelHash = SdrHasher.computeHash(canonicalJson);

        // Generate DDL
        String ddl = generateDdl(model, sqlDialect);

        logger.debug("SDR created: hash={}, ddlLength={}", modelHash, ddl.length());

        return new SdrRecord(canonicalJson, contentType, ddl, modelHash, VERSION);
    }

    @Override
    public String version() {
        return VERSION;
    }

    /**
     * Parses the model source based on content type.
     *
     * @param source the model source string
     * @param contentType the content type
     * @return the parsed SDD model
     * @throws IllegalArgumentException if parsing fails or content type is unsupported
     */
    private SddModel parseModel(String source, String contentType) {
        Try<SddModel> result =
                switch (contentType.toLowerCase()) {
                    case "application/yaml", "text/yaml", "application/x-yaml" -> yamlLoader.loadFromString(source);
                    case "application/json", "text/json" -> jsonLoader.loadFromString(source);
                    default ->
                        Try.failure(new IllegalArgumentException("Unsupported content type: " + contentType
                                + ". Supported: application/yaml, application/json"));
                };

        return result.getOrElseThrow(e -> new IllegalArgumentException("Failed to parse model: " + e.getMessage(), e));
    }

    /**
     * Serializes the model to canonical JSON format.
     *
     * <p>Canonical format ensures stable hashes:
     * <ul>
     *   <li>Sorted keys (alphabetical order)</li>
     *   <li>No formatting/indentation</li>
     *   <li>Consistent encoding</li>
     * </ul>
     *
     * @param model the SDD model
     * @return canonical JSON string
     * @throws IllegalStateException if serialization fails
     */
    private String serializeToCanonicalJson(SddModel model) {
        try {
            StringWriter writer = new StringWriter();
            canonicalMapper.writeValue(writer, model);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize model to canonical JSON", e);
        }
    }

    /**
     * Generates DDL for the model using the specified SQL dialect.
     *
     * @param model the SDD model
     * @param dialect the SQL dialect
     * @return the generated DDL string
     * @throws IllegalArgumentException if dialect is not supported
     */
    private String generateDdl(SddModel model, String dialect) {
        var generator = DdlGenerators.forDialect(dialect);
        return generator.generateDdl(model);
    }

    /**
     * Creates an ObjectMapper configured for canonical JSON output.
     *
     * <p>Configuration:
     * <ul>
     *   <li>ORDER_MAP_ENTRIES_BY_KEYS: alphabetical key sorting</li>
     *   <li>No pretty printing: compact output</li>
     * </ul>
     *
     * @return configured ObjectMapper
     */
    private ObjectMapper createCanonicalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        return mapper;
    }
}
