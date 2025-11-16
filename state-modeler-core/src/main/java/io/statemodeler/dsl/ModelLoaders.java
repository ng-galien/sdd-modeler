package io.statemodeler.dsl;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Factory for creating appropriate ModelLoader instances based on file extensions.
 * Supports automatic detection of YAML and JSON formats.
 */
public final class ModelLoaders {

    private static final Map<String, ModelLoader> loadersByExtension = new HashMap<>();

    static {
        var yamlLoader = new YamlModelLoader();
        loadersByExtension.put("yaml", yamlLoader);
        loadersByExtension.put("yml", yamlLoader);

        var jsonLoader = new JsonModelLoader();
        loadersByExtension.put("json", jsonLoader);
    }

    private ModelLoaders() {
        // Utility class
    }

    /**
     * Get a ModelLoader for the given file extension.
     *
     * @param extension file extension (e.g., "yaml", "yml", "json")
     * @return appropriate ModelLoader, or null if not supported
     */
    public static @Nullable ModelLoader forExtension(String extension) {
        if (extension == null) {
            return null;
        }
        return loadersByExtension.get(extension.toLowerCase());
    }

    /**
     * Get a ModelLoader for the given file path by examining its extension.
     *
     * @param filePath path to the model file
     * @return appropriate ModelLoader, or null if not supported
     */
    public static @Nullable ModelLoader forFile(Path filePath) {
        if (filePath == null) {
            return null;
        }

        String fileName = filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return null;
        }

        String extension = fileName.substring(lastDot + 1);
        return forExtension(extension);
    }

    /**
     * Get the default YAML loader.
     *
     * @return YamlModelLoader instance
     */
    public static YamlModelLoader yaml() {
        return new YamlModelLoader();
    }

    /**
     * Get the default JSON loader.
     *
     * @return JsonModelLoader instance
     */
    public static JsonModelLoader json() {
        return new JsonModelLoader();
    }
}
