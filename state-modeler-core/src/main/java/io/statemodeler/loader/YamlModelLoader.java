package io.statemodeler.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.statemodeler.core.SddModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * ModelLoader implementation for YAML files using Jackson YAML.
 * Supports both .yaml and .yml file extensions.
 */
public final class YamlModelLoader implements ModelLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("yaml", "yml");
    private static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of("application/x-yaml", "application/yaml", "text/x-yaml", "text/yaml");

    private final ObjectMapper mapper;

    public YamlModelLoader() {
        // Configure YAML factory with proper settings
        var yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build();

        this.mapper = new ObjectMapper(yamlFactory);

        // Configure mapper for strict parsing
        mapper.findAndRegisterModules();
    }

    @Override
    public Set<String> supportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public Set<String> supportedContentTypes() {
        return SUPPORTED_CONTENT_TYPES;
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
        try {
            return mapper.readValue(inputStream, SddModel.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse YAML from stream: " + e.getMessage(), e);
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
            throw new IllegalArgumentException("Failed to parse YAML model: " + e.getMessage(), e);
        }
    }

    /**
     * Check if this loader supports the given file extension.
     *
     * @param fileExtension file extension (e.g., "yaml", "yml")
     * @return true if supported
     */
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }
}
