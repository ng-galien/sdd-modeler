package io.statemodeler.sql;

import io.statemodeler.core.SddModel;

/**
 * Interface for generating DDL (Data Definition Language) SQL from SDD models.
 * Implementations provide dialect-specific SQL generation (PostgreSQL, MySQL, etc.).
 */
public interface DdlGenerator {

    /**
     * Generate complete DDL SQL for an SDD model.
     *
     * @param model the SDD model to generate DDL for
     * @return the generated DDL SQL as a string
     * @throws IllegalStateException if DDL generation fails
     */
    String generateDdl(SddModel model);

    /**
     * Get the SQL dialect supported by this generator.
     *
     * @return the dialect name (e.g., "postgres", "mysql")
     */
    String getDialect();

    /**
     * Check if this generator supports the given dialect.
     *
     * @param dialect the dialect name to check
     * @return true if the dialect is supported
     */
    default boolean supportsDialect(String dialect) {
        return getDialect().equalsIgnoreCase(dialect);
    }
}
