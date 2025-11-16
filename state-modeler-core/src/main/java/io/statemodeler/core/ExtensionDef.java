package io.statemodeler.core;

import java.util.Map;
import java.util.Objects;

/**
 * Definition of an extension table for optional, mutable, non-decisional data.
 */
public final class ExtensionDef {
    private final String name;
    private final String table;
    private final String targetState;
    private final Map<String, AttributeDef> attributes;

    public ExtensionDef(String name, String table, String targetState, Map<String, AttributeDef> attributes) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.targetState = Objects.requireNonNull(targetState, "targetState cannot be null");
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes cannot be null"));
    }

    public String name() {
        return name;
    }

    public String table() {
        return table;
    }

    public String targetState() {
        return targetState;
    }

    public Map<String, AttributeDef> attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExtensionDef that = (ExtensionDef) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(table, that.table)
                && Objects.equals(targetState, that.targetState)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, table, targetState, attributes);
    }

    @Override
    public String toString() {
        return "ExtensionDef{"
                + "name='" + name + '\''
                + ", table='" + table + '\''
                + ", targetState='" + targetState + '\''
                + '}';
    }
}
