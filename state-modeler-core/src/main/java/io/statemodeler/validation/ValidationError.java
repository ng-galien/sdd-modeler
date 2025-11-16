package io.statemodeler.validation;

import org.jspecify.annotations.Nullable;

/**
 * Immutable record representing a validation error with context information.
 * Used to accumulate multiple validation errors instead of failing on the first error.
 */
public record ValidationError(
        String code,
        String message,
        @Nullable String entityName,
        @Nullable String stateName,
        @Nullable String fieldName) {

    public ValidationError {
        if (code == null) throw new IllegalArgumentException("code cannot be null");
        if (message == null) throw new IllegalArgumentException("message cannot be null");
    }

    /**
     * Create a validation error for an entity-level issue.
     */
    public static ValidationError entity(String code, String message, String entityName) {
        return new ValidationError(code, message, entityName, null, null);
    }

    /**
     * Create a validation error for a state-level issue.
     */
    public static ValidationError state(String code, String message, String entityName, String stateName) {
        return new ValidationError(code, message, entityName, stateName, null);
    }

    /**
     * Create a validation error for a field-level issue.
     */
    public static ValidationError field(
            String code, String message, String entityName, @Nullable String stateName, String fieldName) {
        return new ValidationError(code, message, entityName, stateName, fieldName);
    }

    /**
     * Create a global validation error (not tied to a specific entity/state).
     */
    public static ValidationError global(String code, String message) {
        return new ValidationError(code, message, null, null, null);
    }

    /**
     * Get a formatted error message with full context.
     */
    public String getFormattedMessage() {
        var builder = new StringBuilder();
        builder.append("[").append(code).append("] ").append(message);

        if (entityName != null) {
            builder.append(" (entity: ").append(entityName);
            if (stateName != null) {
                builder.append(", state: ").append(stateName);
            }
            if (fieldName != null) {
                builder.append(", field: ").append(fieldName);
            }
            builder.append(")");
        } else if (fieldName != null) {
            builder.append(" (field: ").append(fieldName).append(")");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
