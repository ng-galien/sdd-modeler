package io.statemodeler.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of a state in the SDD model.
 * Represents a business state with its attributes, transitions, and metadata.
 */
public final class StateDef {
    private final String name;
    private final String table;
    private final boolean initial;
    private final List<String> from;
    private final List<String> fromAnyOf;
    private final Map<String, AttributeDef> attributes;

    public StateDef(
            String name,
            String table,
            boolean initial,
            List<String> from,
            List<String> fromAnyOf,
            Map<String, AttributeDef> attributes) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.initial = initial;
        this.from = List.copyOf(Objects.requireNonNull(from, "from cannot be null"));
        this.fromAnyOf = List.copyOf(Objects.requireNonNull(fromAnyOf, "fromAnyOf cannot be null"));
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes cannot be null"));
    }

    public String name() {
        return name;
    }

    public String table() {
        return table;
    }

    public boolean isInitial() {
        return initial;
    }

    public List<String> from() {
        return from;
    }

    public List<String> fromAnyOf() {
        return fromAnyOf;
    }

    public Map<String, AttributeDef> attributes() {
        return attributes;
    }

    /**
     * Check if this state has OR transitions (from_any_of).
     */
    public boolean hasOrTransitions() {
        return !fromAnyOf.isEmpty();
    }

    /**
     * Check if this state has simple transitions (from).
     */
    public boolean hasSimpleTransitions() {
        return !from.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StateDef stateDef = (StateDef) obj;
        return initial == stateDef.initial
                && Objects.equals(name, stateDef.name)
                && Objects.equals(table, stateDef.table)
                && Objects.equals(from, stateDef.from)
                && Objects.equals(fromAnyOf, stateDef.fromAnyOf)
                && Objects.equals(attributes, stateDef.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, table, initial, from, fromAnyOf, attributes);
    }

    @Override
    public String toString() {
        return "StateDef{"
                + "name='" + name + '\''
                + ", table='" + table + '\''
                + ", initial=" + initial
                + ", from=" + from
                + ", fromAnyOf=" + fromAnyOf
                + '}';
    }
}
