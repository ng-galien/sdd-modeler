package io.statemodeler.dsl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.statemodeler.core.SddModel;
import io.statemodeler.dsl.yaml.YamlModelConverter;
import io.statemodeler.dsl.yaml.YamlModelDto;
import io.vavr.control.Try;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * ModelLoader implementation for YAML files using Jackson YAML.
 * Supports both .yaml and .yml file extensions.
 * Uses Vavr Try for functional error handling.
 */
public final class YamlModelLoader implements ModelLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("yaml", "yml");
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
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Override
    public Try<SddModel> loadFromFile(Path path) {
        return Try.of(() -> {
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Model file not found: " + path);
            }

            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Path is not a regular file: " + path);
            }

            String content = Files.readString(path);
            return loadFromString(content).get();
        });
    }

    @Override
    public Try<SddModel> loadFromStream(InputStream inputStream) {
        return Try.of(() -> {
            if (inputStream == null) {
                throw new IllegalArgumentException("InputStream cannot be null");
            }

            var dto = mapper.readValue(inputStream, YamlModelDto.class);
            return YamlModelConverter.convert(dto);
        });
    }

    @Override
    public Try<SddModel> loadFromString(String content) {
        return Try.of(() -> {
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Model content cannot be empty");
            }

            var dto = mapper.readValue(content, YamlModelDto.class);
            return YamlModelConverter.convert(dto);
        });
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
