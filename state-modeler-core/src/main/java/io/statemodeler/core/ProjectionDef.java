package io.statemodeler.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Definition of a projection (view) derived from state data.
 */
public record ProjectionDef(
        String name, @JsonProperty("view_name") String viewName, ProjectionKind kind) {

    public ProjectionDef {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (viewName == null) throw new IllegalArgumentException("viewName cannot be null");
        if (kind == null) throw new IllegalArgumentException("kind cannot be null");
    }

    /**
     * Types of projections that can be generated.
     */
    public enum ProjectionKind {
        /** State intervals with start_at/end_at timeline */
        INTERVALS,
        /** Current active states only */
        CURRENT_STATE
    }
}
