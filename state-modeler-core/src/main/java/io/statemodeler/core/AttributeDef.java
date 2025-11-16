package io.statemodeler.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Definition of an attribute (column) in a table.
 * Used for both entity attributes and state-specific attributes.
 */
public record AttributeDef(
        String name,
        String type,
        boolean nullable,
        @JsonProperty("primary_key") boolean primaryKey,
        @JsonProperty("default_value") @Nullable String defaultValue,
        @Nullable String description) {

    public AttributeDef {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        // defaultValue and description can be null
    }
}
