package io.statemodeler.core;

import java.util.Map;

/**
 * Root model representing a complete SDD (State-Driven Design) specification.
 * Contains all entities, their states, transitions, extensions, and projections.
 */
public record SddModel(String version, String name, DatabaseConfig database, Map<String, EntityDef> entities) {

    public SddModel(String version, String name, DatabaseConfig database, Map<String, EntityDef> entities) {
        if (version == null) throw new IllegalArgumentException("version cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (database == null) throw new IllegalArgumentException("database cannot be null");
        if (entities == null) throw new IllegalArgumentException("entities cannot be null");

        this.version = version;
        this.name = name;
        this.database = database;
        this.entities = Map.copyOf(entities);
    }
}
