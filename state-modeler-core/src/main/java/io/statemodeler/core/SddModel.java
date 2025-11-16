package io.statemodeler.core;

import java.util.Map;
import java.util.Objects;

/**
 * Root model representing a complete SDD (State-Driven Design) specification.
 * Contains all entities, their states, transitions, extensions, and projections.
 */
public final class SddModel {
    private final String version;
    private final String name;
    private final DatabaseConfig database;
    private final Map<String, EntityDef> entities;

    public SddModel(String version, String name, DatabaseConfig database, Map<String, EntityDef> entities) {
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.database = Objects.requireNonNull(database, "database cannot be null");
        this.entities = Map.copyOf(Objects.requireNonNull(entities, "entities cannot be null"));
    }

    public String version() {
        return version;
    }

    public String name() {
        return name;
    }

    public DatabaseConfig database() {
        return database;
    }

    public Map<String, EntityDef> entities() {
        return entities;
    }

    public EntityDef getEntity(String name) {
        return entities.get(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SddModel sddModel = (SddModel) obj;
        return Objects.equals(version, sddModel.version)
                && Objects.equals(name, sddModel.name)
                && Objects.equals(database, sddModel.database)
                && Objects.equals(entities, sddModel.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name, database, entities);
    }

    @Override
    public String toString() {
        return "SddModel{"
                + "version='" + version + '\''
                + ", name='" + name + '\''
                + ", database=" + database
                + ", entities=" + entities.keySet()
                + '}';
    }
}
