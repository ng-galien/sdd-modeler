package io.statemodeler.sql;

import io.statemodeler.sql.postgres.PebblePostgresDdlGenerator;

/**
 * Factory for creating DDL generators based on SQL dialect.
 */
public final class DdlGenerators {

    private DdlGenerators() {
        // Utility class
    }

    /**
     * Create a DDL generator for the specified dialect.
     *
     * @param dialect the SQL dialect (e.g., "postgres")
     * @return the appropriate DDL generator
     * @throws IllegalArgumentException if the dialect is not supported
     */
    public static DdlGenerator forDialect(String dialect) {
        return switch (dialect.toLowerCase()) {
            case "postgres", "postgresql" -> new PebblePostgresDdlGenerator();
            default -> throw new IllegalArgumentException("Unsupported SQL dialect: " + dialect);
        };
    }

    /**
     * Get all supported SQL dialects.
     *
     * @return array of supported dialect names
     */
    public static String[] getSupportedDialects() {
        return new String[] {"postgres"};
    }

    /**
     * Check if a dialect is supported.
     *
     * @param dialect the dialect to check
     * @return true if supported
     */
    public static boolean isSupported(String dialect) {
        try {
            forDialect(dialect);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
