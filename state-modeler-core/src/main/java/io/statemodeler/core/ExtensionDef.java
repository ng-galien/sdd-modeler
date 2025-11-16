package io.statemodeler.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Definition of an extension table for optional, mutable, non-decisional data.
 */
public record ExtensionDef(
        String name,
        String table,
        @JsonProperty("target_state") String targetState,
        Map<String, AttributeDef> attributes) {

    public ExtensionDef {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        if (targetState == null) throw new IllegalArgumentException("targetState cannot be null");
        if (attributes == null) throw new IllegalArgumentException("attributes cannot be null");
    }
}
