package io.statemodeler.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.statemodeler.core.SddModel;
import io.statemodeler.dsl.yaml.YamlModelConverter;
import io.statemodeler.dsl.yaml.YamlModelDto;
import io.vavr.control.Try;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * ModelLoader implementation for JSON files using Jackson.
 * Uses the same DTO structure as YAML loader for consistency.
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
