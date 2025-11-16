package io.statemodeler.migration;

import io.vavr.control.Try;

/**
 * Service interface for generating SQL migration scripts from DDL differences.
 *
 * <p>This service uses LLM-based analysis to produce forward migration scripts
 * that safely transform a database schema from an old DDL to a new DDL.
 */
public interface MigrationGenerationService {

    /**
     * Generates a SQL migration script to transform the old DDL into the new DDL.
     *
     * <p>The implementation uses an LLM to analyze the differences and produce
     * a safe, forward-compatible migration script with the following constraints:
     * <ul>
     *   <li>Avoid data loss where possible</li>
     *   <li>Use ALTER TABLE, ADD COLUMN, etc. instead of DROP TABLE when applicable</li>
     *   <li>Generate readable SQL with inline comments</li>
     * </ul>
     *
     * @param oldDdl the original/current DDL schema (cannot be null)
     * @param newDdl the target/future DDL schema (cannot be null)
     * @param textDiff the unified diff between old and new DDL (cannot be null)
     * @param dialect the SQL dialect (e.g., "postgres", "mysql") (cannot be null)
     * @return Try containing the generated SQL migration script, or Failure if generation fails
     * @throws IllegalArgumentException if any parameter is null
     */
    Try<String> generateMigrationScript(String oldDdl, String newDdl, String textDiff, String dialect);
}
