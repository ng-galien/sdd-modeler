package io.statemodeler.core;

import java.util.Objects;

/**
 * Definition of an attribute (column) in a table.
 * Used for both entity attributes and state-specific attributes.
 */
public final class AttributeDef {
    private final String name;
    private final String type;
    private final boolean nullable;
    private final boolean primaryKey;
    private final String defaultValue;
    private final String description;

    public AttributeDef(
            String name, String type, boolean nullable, boolean primaryKey, String defaultValue, String description) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.defaultValue = defaultValue; // Can be null
        this.description = description; // Can be null
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public boolean nullable() {
        return nullable;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AttributeDef that = (AttributeDef) obj;
        return nullable == that.nullable
                && primaryKey == that.primaryKey
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, primaryKey, defaultValue, description);
    }

    @Override
    public String toString() {
        return "AttributeDef{"
                + "name='" + name + '\''
                + ", type='" + type + '\''
                + ", nullable=" + nullable
                + ", primaryKey=" + primaryKey
                + '}';
    }
}
