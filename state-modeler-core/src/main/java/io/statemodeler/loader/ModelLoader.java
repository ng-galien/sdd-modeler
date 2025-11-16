package io.statemodeler.loader;

import io.statemodeler.core.SddModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

/**
 * Interface for loading SDD models from various formats (YAML, JSON).
 * <p>
 * Implementations are discovered via the Java Service Provider Interface (SPI)
 * using {@link java.util.ServiceLoader}. Each implementation must provide a
 * public no-arg constructor and be registered under
 * {@code META-INF/services/io.statemodeler.dsl.ModelLoader}.
 * </p>
 */
public interface ModelLoader {

    /**
     * Load an SDD model from a file path.
     * @param path the path to the model file
     * @return the loaded SDD model
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the model format is invalid
     */
    SddModel loadFromFile(Path path) throws IOException;

    /**
     * Load an SDD model from an input stream.
     * @param inputStream the input stream containing the model
     * @return the loaded SDD model
     * @throws IOException if the stream cannot be read
     * @throws IllegalArgumentException if the model format is invalid
     */
    SddModel loadFromStream(InputStream inputStream) throws IOException;

    /**
     * Load an SDD model from a string.
     * @param content the string content containing the model
     * @return the loaded SDD model
     * @throws IllegalArgumentException if the model format is invalid
     */
    SddModel loadFromString(String content);

    /**
     * Return the set of supported file extensions (without dot, lower-case),
     * e.g. {@code "yaml"}, {@code "yml"}, {@code "json"}.
     * <p>
     * Default implementation returns an empty set, meaning the loader cannot
     * be selected by extension. Implementations should override this to
     * advertise the formats they can handle.
     * </p>
     *
     * @return supported file extensions
     */
    default Set<String> supportedExtensions() {
        return Set.of();
    }

    /**
     * Return the set of supported content types (MIME types), e.g.
     * {@code "application/yaml"}, {@code "application/json"}.
     * <p>
     * Default implementation returns an empty set. This hook is reserved for
     * future use where callers may want to resolve loaders by content type.
     * </p>
     *
     * @return supported content types
     */
    default Set<String> supportedContentTypes() {
        return Set.of();
    }
}
