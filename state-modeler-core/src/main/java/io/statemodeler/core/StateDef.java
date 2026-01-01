package io.statemodeler.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        @JsonProperty("from") String from,
        @JsonProperty("from_any_of") List<String> fromAnyOf,
        Map<String, AttributeDef> attributes) {

    public StateDef(
            String name,
            String table,
            boolean initial,
            String from,
            List<String> fromAnyOf,
            Map<String, AttributeDef> attributes) {

        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        if (fromAnyOf == null) throw new IllegalArgumentException("fromAnyOf cannot be null");
        if (attributes == null) throw new IllegalArgumentException("attributes cannot be null");

        this.name = name;
        this.table = table;
        this.initial = initial;
        this.from = (from != null && !from.isBlank()) ? from : null;
        this.fromAnyOf = List.copyOf(fromAnyOf);
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * Backward-compatible constructor accepting the legacy list form.
     * Enforces that at most one predecessor is provided.
     */
    public StateDef(
            String name,
            String table,
            boolean initial,
            List<String> from,
            List<String> fromAnyOf,
            Map<String, AttributeDef> attributes) {
        this(
                name,
                table,
                initial,
                (from == null || from.isEmpty()) ? null : (from.size() == 1 ? from.get(0) : throwMultiFrom()),
                fromAnyOf == null ? List.of() : fromAnyOf,
                attributes);
    }

    private static String throwMultiFrom() {
        throw new IllegalArgumentException("state.from supports a single predecessor; use from_any_of for multiples");
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
        return from != null && !from.isBlank();
    }
}
