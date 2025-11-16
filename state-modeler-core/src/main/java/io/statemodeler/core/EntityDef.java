package io.statemodeler.core;

import java.util.Map;

/**
 * Definition of an entity in the SDD model.
 * Represents stable, non-state-specific data and the entity's state graph.
 */
public record EntityDef(
        String name,
        String table,
        AttributeDef id,
        Map<String, AttributeDef> attributes,
        Map<String, StateDef> states,
        Map<String, ExtensionDef> extensions,
        Map<String, ProjectionDef> projections) {

    public EntityDef(
            String name,
            String table,
            AttributeDef id,
            Map<String, AttributeDef> attributes,
            Map<String, StateDef> states,
            Map<String, ExtensionDef> extensions,
            Map<String, ProjectionDef> projections) {

        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (attributes == null) throw new IllegalArgumentException("attributes cannot be null");
        if (states == null) throw new IllegalArgumentException("states cannot be null");
        if (extensions == null) throw new IllegalArgumentException("extensions cannot be null");
        if (projections == null) throw new IllegalArgumentException("projections cannot be null");

        this.name = name;
        this.table = table;
        this.id = id;
        this.attributes = Map.copyOf(attributes);
        this.states = Map.copyOf(states);
        this.extensions = Map.copyOf(extensions);
        this.projections = Map.copyOf(projections);
    }

    /**
     * Find the initial state for this entity.
     * @return the initial state, or null if none is marked as initial
     */
    public StateDef findInitialState() {
        return states.values().stream().filter(StateDef::initial).findFirst().orElse(null);
    }
}
