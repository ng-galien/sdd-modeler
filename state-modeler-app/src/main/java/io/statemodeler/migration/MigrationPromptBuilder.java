package io.statemodeler.migration;

/**
 * Helper class to build LLM prompts for SQL migration script generation.
 *
 * <p>Constructs a detailed prompt containing:
 * <ul>
 *   <li>System instructions (role, dialect, constraints)</li>
 *   <li>Unified diff between old and new DDL</li>
 *   <li>Complete old DDL</li>
 *   <li>Complete new DDL</li>
 * </ul>
 */
final class MigrationPromptBuilder {

    private MigrationPromptBuilder() {
        // Utility class - no instantiation
    }

    /**
     * Builds the complete prompt for the LLM.
     *
     * @param oldDdl the original DDL schema
     * @param newDdl the target DDL schema
     * @param textDiff the unified diff
     * @param dialect the SQL dialect (e.g., "postgres", "mysql")
     * @return the formatted prompt as a String
     */
    static String buildPrompt(String oldDdl, String newDdl, String textDiff, String dialect) {
        if (oldDdl == null) {
            throw new IllegalArgumentException("oldDdl cannot be null");
        }
        if (newDdl == null) {
            throw new IllegalArgumentException("newDdl cannot be null");
        }
        if (textDiff == null) {
            throw new IllegalArgumentException("textDiff cannot be null");
        }
        if (dialect == null) {
            throw new IllegalArgumentException("dialect cannot be null");
        }

        return """
                # ROLE
                You are an expert database migration specialist.

                # OBJECTIVE
                Generate a forward SQL migration script to transform the OLD DDL into the NEW DDL.

                # CONSTRAINTS
                - SQL Dialect: %s
                - Avoid data loss whenever possible
                - Use ALTER TABLE, ADD COLUMN, MODIFY COLUMN, etc. instead of DROP TABLE when feasible
                - Provide clear inline comments (-- style) explaining each major step
                - Focus on safety and backward compatibility
                - If a column is removed, consider commenting it out with a note instead of dropping it immediately

                # OUTPUT FORMAT
                Return ONLY the SQL migration script.
                You may include a brief commented header explaining the migration strategy.
                Do NOT include explanations outside of SQL comments.

                # DIFF (unified format)
                ```diff
                %s
                ```

                # OLD DDL
                ```sql
                %s
                ```

                # NEW DDL
                ```sql
                %s
                ```

                Generate the migration script now:
                """.formatted(dialect.toUpperCase(), textDiff, oldDdl, newDdl);
    }
}
