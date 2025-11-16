package io.statemodeler.sql.postgres;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates PostgreSQL data types.
 * Ensures that types used in the DSL are valid PostgreSQL types.
 */
public final class PostgresTypeValidator {

    // Common PostgreSQL numeric types
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "SMALLINT",
            "INTEGER",
            "INT",
            "BIGINT",
            "DECIMAL",
            "NUMERIC",
            "REAL",
            "DOUBLE PRECISION",
            "SMALLSERIAL",
            "SERIAL",
            "BIGSERIAL");

    // Common PostgreSQL character types
    private static final Set<String> CHARACTER_TYPES = Set.of("CHAR", "VARCHAR", "TEXT");

    // Common PostgreSQL binary types
    private static final Set<String> BINARY_TYPES = Set.of("BYTEA");

    // Common PostgreSQL date/time types
    private static final Set<String> DATETIME_TYPES = Set.of(
            "TIMESTAMP",
            "TIMESTAMPTZ",
            "TIMESTAMP WITH TIME ZONE",
            "TIMESTAMP WITHOUT TIME ZONE",
            "DATE",
            "TIME",
            "TIMETZ",
            "TIME WITH TIME ZONE",
            "TIME WITHOUT TIME ZONE",
            "INTERVAL");

    // Common PostgreSQL boolean type
    private static final Set<String> BOOLEAN_TYPES = Set.of("BOOLEAN", "BOOL");

    // Common PostgreSQL JSON types
    private static final Set<String> JSON_TYPES = Set.of("JSON", "JSONB");

    // Common PostgreSQL UUID type
    private static final Set<String> UUID_TYPES = Set.of("UUID");

    // Common PostgreSQL array types (handled via pattern)
    private static final Pattern ARRAY_TYPE_PATTERN = Pattern.compile("^[A-Z]+\\[\\]$", Pattern.CASE_INSENSITIVE);

    // Parameterized types patterns
    private static final Pattern NUMERIC_WITH_PRECISION_PATTERN =
            Pattern.compile("^(NUMERIC|DECIMAL)\\(\\d+,\\s*\\d+\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARCHAR_WITH_LENGTH_PATTERN =
            Pattern.compile("^(VARCHAR|CHAR)\\(\\d+\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMESTAMP_WITH_PRECISION_PATTERN =
            Pattern.compile("^(TIMESTAMP|TIMESTAMPTZ|TIME|TIMETZ)\\(\\d+\\)$", Pattern.CASE_INSENSITIVE);

    private PostgresTypeValidator() {
        // Utility class
    }

    /**
     * Validates if a type string is a valid PostgreSQL type.
     *
     * @param type The type string to validate (case-insensitive)
     * @return true if the type is valid, false otherwise
     */
    public static boolean isValidType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }

        var normalizedType = type.trim().toUpperCase();

        // Check simple types
        if (NUMERIC_TYPES.contains(normalizedType)
                || CHARACTER_TYPES.contains(normalizedType)
                || BINARY_TYPES.contains(normalizedType)
                || DATETIME_TYPES.contains(normalizedType)
                || BOOLEAN_TYPES.contains(normalizedType)
                || JSON_TYPES.contains(normalizedType)
                || UUID_TYPES.contains(normalizedType)) {
            return true;
        }

        // Check array types
        if (ARRAY_TYPE_PATTERN.matcher(type).matches()) {
            return true;
        }

        // Check parameterized types
        if (NUMERIC_WITH_PRECISION_PATTERN.matcher(type).matches()
                || VARCHAR_WITH_LENGTH_PATTERN.matcher(type).matches()
                || TIMESTAMP_WITH_PRECISION_PATTERN.matcher(type).matches()) {
            return true;
        }

        return false;
    }

    /**
     * Get a human-readable error message for an invalid type.
     *
     * @param type The invalid type
     * @return Error message explaining why the type is invalid
     */
    public static String getErrorMessage(String type) {
        return "Invalid PostgreSQL type: '" + type
                + "'. Expected types like TEXT, INTEGER, BIGINT, NUMERIC(p,s), TIMESTAMPTZ, JSONB, etc.";
    }
}
