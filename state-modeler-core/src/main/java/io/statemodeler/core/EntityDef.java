package io.statemodeler.core;

import java.util.Map;
import java.util.Objects;

/**
 * Definition of an entity in the SDD model.
 * Represents stable, non-state-specific data and the entity's state graph.
 */
public final class EntityDef {
    private final String name;
    private final String table;
    private final AttributeDef id;
    private final Map<String, AttributeDef> attributes;
    private final Map<String, StateDef> states;
    private final Map<String, ExtensionDef> extensions;
    private final Map<String, ProjectionDef> projections;

    public EntityDef(
            String name,
            String table,
            AttributeDef id,
            Map<String, AttributeDef> attributes,
            Map<String, StateDef> states,
            Map<String, ExtensionDef> extensions,
            Map<String, ProjectionDef> projections) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes cannot be null"));
        this.states = Map.copyOf(Objects.requireNonNull(states, "states cannot be null"));
        this.extensions = Map.copyOf(Objects.requireNonNull(extensions, "extensions cannot be null"));
        this.projections = Map.copyOf(Objects.requireNonNull(projections, "projections cannot be null"));
    }

    public String name() {
        return name;
    }

    public String table() {
        return table;
    }

    public AttributeDef id() {
        return id;
    }

    public Map<String, AttributeDef> attributes() {
        return attributes;
    }

    public Map<String, StateDef> states() {
        return states;
    }

    public Map<String, ExtensionDef> extensions() {
        return extensions;
    }

    public Map<String, ProjectionDef> projections() {
        return projections;
    }

    /**
     * Find the initial state for this entity.
     * @return the initial state, or null if none is marked as initial
     */
    public StateDef findInitialState() {
        return states.values().stream().filter(StateDef::isInitial).findFirst().orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntityDef entityDef = (EntityDef) obj;
        return Objects.equals(name, entityDef.name)
                && Objects.equals(table, entityDef.table)
                && Objects.equals(id, entityDef.id)
                && Objects.equals(attributes, entityDef.attributes)
                && Objects.equals(states, entityDef.states)
                && Objects.equals(extensions, entityDef.extensions)
                && Objects.equals(projections, entityDef.projections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, table, id, attributes, states, extensions, projections);
    }

    @Override
    public String toString() {
        return "EntityDef{" + "name='" + name + '\'' + ", table='" + table + '\'' + ", states=" + states.keySet() + '}';
    }
}
