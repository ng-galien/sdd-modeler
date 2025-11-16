package io.statemodeler.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.statemodeler.core.SddModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * ModelLoader implementation for JSON files using Jackson.
 */
public final class JsonModelLoader implements ModelLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("json");
    private final ObjectMapper mapper;

    public JsonModelLoader() {
        this.mapper = new ObjectMapper();

        // Configure mapper for strict parsing
        mapper.findAndRegisterModules();
    }

    @Override
    public SddModel loadFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Model file not found: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("Path is not a regular file: " + path);
        }

        try {
            String content = Files.readString(path);
            return loadFromString(content);
        } catch (IOException e) {
            throw new IOException("Failed to read model file: " + path, e);
        }
    }

    @Override
    public SddModel loadFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            return mapper.readValue(inputStream, SddModel.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON from stream: " + e.getMessage(), e);
        }
    }

    @Override
    public SddModel loadFromString(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Model content cannot be empty");
        }

        try {
            return mapper.readValue(content, SddModel.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON model: " + e.getMessage(), e);
        }
    }

    /**
     * Check if this loader supports the given file extension.
     *
     * @param fileExtension file extension (e.g., "json")
     * @return true if supported
     */
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }
}
