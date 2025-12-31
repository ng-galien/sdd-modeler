package io.statemodeler.diagram.mermaid;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.diagram.DiagramGenerator;
import java.util.stream.Collectors;

/**
 * Generates Mermaid state diagrams from SDD models.
 * Uses Mermaid's stateDiagram-v2 syntax for visualizing state transitions.
 *
 * @see <a href="https://mermaid.js.org/syntax/stateDiagram.html">Mermaid State Diagrams</a>
 */
public final class MermaidDiagramGenerator implements DiagramGenerator {

    @Override
    public String generateDiagram(SddModel model) {
        var diagram = new StringBuilder();

        // Generate diagram for each entity
        for (var entry : model.entities().entrySet()) {
            var entityName = entry.getKey();
            var entity = entry.getValue();

            if (!diagram.isEmpty()) {
                diagram.append("\n\n");
            }

            diagram.append(generateEntityDiagram(entityName, entity));
        }

        return diagram.toString();
    }

    @Override
    public String generateDiagram(SddModel model, String entityName) {
        var entity = model.entities().get(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("Entity not found: " + entityName);
        }

        return generateEntityDiagram(entityName, entity);
    }

    @Override
    public String getFormat() {
        return "mermaid";
    }

    private String generateEntityDiagram(String entityName, EntityDef entity) {
        var diagram = new StringBuilder();

        // Diagram header
        diagram.append("---\n");
        diagram.append("title: ").append(entityName).append(" State Diagram\n");
        diagram.append("---\n");
        diagram.append("stateDiagram-v2\n");

        // Find initial state
        var initialState = entity.states().entrySet().stream()
                .filter(e -> e.getValue().initial())
                .findFirst()
                .orElse(null);

        if (initialState != null) {
            diagram.append("    [*] --> ").append(initialState.getKey()).append("\n");
        }

        // Generate state definitions with notes for attributes
        for (var stateEntry : entity.states().entrySet()) {
            var stateName = stateEntry.getKey();
            var state = stateEntry.getValue();

            // Add state with description if it has attributes
            if (!state.attributes().isEmpty()) {
                diagram.append("    ").append(stateName).append(" : ");
                var attrs = state.attributes().keySet().stream()
                        .limit(3) // Show first 3 attributes
                        .collect(Collectors.joining(", "));
                diagram.append(attrs);
                if (state.attributes().size() > 3) {
                    diagram.append(", ...");
                }
                diagram.append("\n");
            }
        }

        // Generate transitions
        for (var stateEntry : entity.states().entrySet()) {
            var stateName = stateEntry.getKey();
            var state = stateEntry.getValue();

            // Simple transitions (from)
            if (state.from() != null && !state.from().isBlank()) {
                diagram.append("    ")
                        .append(state.from())
                        .append(" --> ")
                        .append(stateName)
                        .append("\n");
            }

            // OR transitions (from_any_of)
            if (state.fromAnyOf() != null && !state.fromAnyOf().isEmpty()) {
                for (var sourceState : state.fromAnyOf()) {
                    diagram.append("    ")
                            .append(sourceState)
                            .append(" --> ")
                            .append(stateName)
                            .append(" : OR\n");
                }
            }
        }

        // Add notes for extensions, grouped by target state
        if (entity.extensions() != null && !entity.extensions().isEmpty()) {
            // Group extensions by their target state
            var extensionsByState =
                    entity.extensions().values().stream().collect(Collectors.groupingBy(ext -> ext.targetState()));
            for (var entry : extensionsByState.entrySet()) {
                var stateName = entry.getKey();
                var stateExtensions =
                        entry.getValue().stream().map(ext -> ext.name()).collect(Collectors.joining(", "));
                diagram.append("\n    note right of ").append(stateName).append("\n");
                diagram.append("        Extensions: ").append(stateExtensions);
                diagram.append("\n    end note\n");
            }
        }

        return diagram.toString();
    }
}
