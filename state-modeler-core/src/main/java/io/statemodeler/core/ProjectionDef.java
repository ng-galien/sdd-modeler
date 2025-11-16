package io.statemodeler.core;

import java.util.Objects;

/**
 * Definition of a projection (view) derived from state data.
 */
public final class ProjectionDef {
    private final String name;
    private final String viewName;
    private final ProjectionKind kind;

    public ProjectionDef(String name, String viewName, ProjectionKind kind) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.viewName = Objects.requireNonNull(viewName, "viewName cannot be null");
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    }

    public String name() {
        return name;
    }

    public String viewName() {
        return viewName;
    }

    public ProjectionKind kind() {
        return kind;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProjectionDef that = (ProjectionDef) obj;
        return Objects.equals(name, that.name) && Objects.equals(viewName, that.viewName) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, viewName, kind);
    }

    @Override
    public String toString() {
        return "ProjectionDef{" + "name='" + name + '\'' + ", viewName='" + viewName + '\'' + ", kind=" + kind + '}';
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
