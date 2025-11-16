package io.statemodeler.diagram;

import io.statemodeler.core.SddModel;

/**
 * Interface for generating state diagrams from SDD models.
 * Implementations provide format-specific diagram generation (Mermaid, PlantUML, etc.).
 */
public interface DiagramGenerator {

    /**
     * Generate a complete diagram for an SDD model.
     *
     * @param model the SDD model to generate diagram for
     * @return the generated diagram as a string
     * @throws IllegalStateException if diagram generation fails
     */
    String generateDiagram(SddModel model);

    /**
     * Generate a diagram for a specific entity within an SDD model.
     *
     * @param model the SDD model containing the entity
     * @param entityName the name of the entity to generate diagram for
     * @return the generated diagram as a string
     * @throws IllegalArgumentException if entity not found
     * @throws IllegalStateException if diagram generation fails
     */
    String generateDiagram(SddModel model, String entityName);

    /**
     * Get the diagram format supported by this generator.
     *
     * @return the format name (e.g., "mermaid", "plantuml")
     */
    String getFormat();

    /**
     * Check if this generator supports the given format.
     *
     * @param format the format name to check
     * @return true if the format is supported
     */
    default boolean supports(String format) {
        return getFormat().equalsIgnoreCase(format);
    }
}
