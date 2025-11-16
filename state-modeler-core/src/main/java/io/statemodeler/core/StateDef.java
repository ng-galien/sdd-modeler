package io.statemodeler.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Definition of a state in the SDD model.
 * Represents a business state with its attributes, transitions, and metadata.
 */
public record StateDef(
        String name,
        String table,
        boolean initial,
        List<String> from,
        @JsonProperty("from_any_of") List<String> fromAnyOf,
        Map<String, AttributeDef> attributes) {

    public StateDef(
            String name,
            String table,
            boolean initial,
            List<String> from,
            List<String> fromAnyOf,
            Map<String, AttributeDef> attributes) {

        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        if (from == null) throw new IllegalArgumentException("from cannot be null");
        if (fromAnyOf == null) throw new IllegalArgumentException("fromAnyOf cannot be null");
        if (attributes == null) throw new IllegalArgumentException("attributes cannot be null");

        this.name = name;
        this.table = table;
        this.initial = initial;
        this.from = List.copyOf(from);
        this.fromAnyOf = List.copyOf(fromAnyOf);
        this.attributes = Map.copyOf(attributes);
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
}
