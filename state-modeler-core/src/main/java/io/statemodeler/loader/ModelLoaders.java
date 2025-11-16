package io.statemodeler.loader;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Factory for discovering and selecting {@link ModelLoader} implementations
 * using Java SPI (ServiceLoader).
 * <p>
 * Implementations of {@link ModelLoader} must be registered under
 * {@code META-INF/services/io.statemodeler.loader.ModelLoader}.
 * </p>
 */
public final class ModelLoaders {

    private ModelLoaders() {
        // Utility class
    }

    private static Stream<ModelLoader> loadAll() {
        return ServiceLoader.load(ModelLoader.class).stream()
                .map(ServiceLoader.Provider::get);
    }

    /**
     * Get a ModelLoader for the given file extension.
     *
     * @param extension file extension (e.g., "yaml", "yml", "json")
     * @return appropriate ModelLoader, or null if not supported
     */
    public static ModelLoader forExtension(String extension) {
        var normalized = extension.toLowerCase();
        return loadAll()
                .filter(loader -> loader.supportedExtensions().contains(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No ModelLoader found for extension: " + extension));
    }

    /**
     * Get a ModelLoader for the given file path by examining its extension.
     *
     * @param filePath path to the model file
     * @return appropriate ModelLoader, or null if not supported
     */
    public static ModelLoader forFile(Path filePath) {
        var fileName = filePath.getFileName().toString();
        var lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            throw new IllegalArgumentException("File has no extension: " + fileName);
        }

        var extension = fileName.substring(lastDot + 1);
        return forExtension(extension);
    }
}
