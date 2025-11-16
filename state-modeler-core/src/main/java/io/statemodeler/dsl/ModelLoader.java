package io.statemodeler.dsl;

import io.statemodeler.core.SddModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for loading SDD models from various formats (YAML, JSON).
 */
public interface ModelLoader {

    /**
     * Load an SDD model from a file path.
     * @param path the path to the model file
     * @return the loaded SDD model
     * @throws IOException if the file cannot be read
     * @throws ModelParsingException if the model format is invalid
     */
    SddModel loadFromFile(Path path) throws IOException, ModelParsingException;

    /**
     * Load an SDD model from an input stream.
     * @param inputStream the input stream containing the model
     * @return the loaded SDD model
     * @throws IOException if the stream cannot be read
     * @throws ModelParsingException if the model format is invalid
     */
    SddModel loadFromStream(InputStream inputStream) throws IOException, ModelParsingException;

    /**
     * Load an SDD model from a string.
     * @param content the string content containing the model
     * @return the loaded SDD model
     * @throws ModelParsingException if the model format is invalid
     */
    SddModel loadFromString(String content) throws ModelParsingException;
}
