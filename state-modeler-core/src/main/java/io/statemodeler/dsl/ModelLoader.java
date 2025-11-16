package io.statemodeler.dsl;

import io.statemodeler.core.SddModel;
import io.vavr.control.Try;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for loading SDD models from various formats (YAML, JSON).
 * Uses Vavr Try for functional error handling instead of checked exceptions.
 */
public interface ModelLoader {

    /**
     * Load an SDD model from a file path.
     * @param path the path to the model file
     * @return Try containing either the loaded SDD model or an exception
     */
    Try<SddModel> loadFromFile(Path path);

    /**
     * Load an SDD model from an input stream.
     * @param inputStream the input stream containing the model
     * @return Try containing either the loaded SDD model or an exception
     */
    Try<SddModel> loadFromStream(InputStream inputStream);

    /**
     * Load an SDD model from a string.
     * @param content the string content containing the model
     * @return Try containing either the loaded SDD model or an exception
     */
    Try<SddModel> loadFromString(String content);

    /**
     * Factory method to create the appropriate loader based on file extension.
     * @param file the model file path
     * @return ModelLoader instance for the file type
     * @throws IllegalArgumentException if the file type is not supported
     */
    static ModelLoader forFile(Path file) {
        var fileName = file.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return new YamlModelLoader();
        } else if (fileName.endsWith(".json")) {
            return new JsonModelLoader();
        } else {
            throw new IllegalArgumentException("Unsupported file extension. Supported: .yaml, .yml, .json");
        }
    }
}
