package io.statemodeler.diagram;

import io.statemodeler.diagram.mermaid.MermaidDiagramGenerator;

/**
 * Factory for creating diagram generators based on format.
 */
public final class DiagramGenerators {

    private DiagramGenerators() {
        // Utility class
    }

    /**
     * Create a diagram generator for the specified format.
     *
     * @param format the diagram format (e.g., "mermaid", "plantuml")
     * @return the appropriate diagram generator
     * @throws IllegalArgumentException if the format is not supported
     */
    public static DiagramGenerator forFormat(String format) {
        return switch (format.toLowerCase()) {
            case "mermaid" -> new MermaidDiagramGenerator();
            default -> throw new IllegalArgumentException("Unsupported diagram format: " + format);
        };
    }

    /**
     * Get all supported diagram formats.
     *
     * @return array of supported format names
     */
    public static String[] getSupportedFormats() {
        return new String[] {"mermaid"};
    }

    /**
     * Check if a format is supported.
     *
     * @param format the format to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(String format) {
        if (format == null) {
            return false;
        }
        return switch (format.toLowerCase()) {
            case "mermaid" -> true;
            default -> false;
        };
    }
}
