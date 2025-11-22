package io.statemodeler.sql;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.statemodeler.core.SddModel;

/**
 * Interface for generating DDL (Data Definition Language) SQL from SDD models.
 * Implementations provide dialect-specific SQL generation (PostgreSQL, MySQL,
 * etc.).
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
     * Generate complete DDL SQL for an SDD model with formatting applied.
     * This method formats the DDL output for better readability using
     * sql-formatter.
     *
     * @param model the SDD model to generate DDL for
     * @return the formatted DDL SQL as a string
     * @throws IllegalStateException if DDL generation fails
     */
    default String generateFormattedDdl(SddModel model) {
        String ddl = generateDdl(model);
        return SqlFormatter.of(Dialect.PostgreSql).format(ddl);
    }

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
