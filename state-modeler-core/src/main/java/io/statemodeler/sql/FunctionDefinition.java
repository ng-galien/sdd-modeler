package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Represents a SQL function definition.
 *
 * <p>
 * This is a dialect-agnostic representation that can be rendered to specific
 * SQL dialects.
 *
 * @param name       function name (without schema)
 * @param schema     schema name where the function will be created
 * @param parameters list of parameter definitions (e.g., "param_name
 *                   param_type")
 * @param returnType return type of the function (e.g., "TRIGGER", "INTEGER",
 *                   "TEXT")
 * @param language   function language (e.g., "plpgsql", "sql")
 * @param body       function body/implementation
 */
public record FunctionDefinition(
        String name, String schema, List<String> parameters, String returnType, String language, String body) {

    public FunctionDefinition {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        Objects.requireNonNull(schema, "schema cannot be null");
        if (schema.isBlank()) {
            throw new IllegalArgumentException("schema cannot be blank");
        }

        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters cannot be null"));

        Objects.requireNonNull(returnType, "returnType cannot be null");
        if (returnType.isBlank()) {
            throw new IllegalArgumentException("returnType cannot be blank");
        }

        Objects.requireNonNull(language, "language cannot be null");
        if (language.isBlank()) {
            throw new IllegalArgumentException("language cannot be blank");
        }

        Objects.requireNonNull(body, "body cannot be null");
        if (body.isBlank()) {
            throw new IllegalArgumentException("body cannot be blank");
        }
    }

    /**
     * Get the fully qualified function name (schema.name).
     *
     * @return fully qualified name
     */
    public String fullName() {
        return schema + "." + name;
    }
}
