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

        // Use a structured-output friendly prompt and an example JSON output. Although the
        // AI Service will use JSON Schema when available, providing an explicit example
        // improves the model's likelihood to output the expected format when JSON Schema
        // is not enabled by the provider.
        return """
                                SYSTEM: You are an expert database migration specialist. Reply only with a JSON object
                                conforming to the described structure -- do NOT include extra text outside the JSON.

                                TASK: Generate a forward SQL migration script to transform the OLD DDL into the NEW DDL.

                                CONSTRAINTS:
                                - SQL Dialect: %s
                                - Avoid data loss whenever possible.
                                - Prefer ALTER TABLE / ADD COLUMN / ALTER COLUMN over DROP TABLE when feasible.
                                - Add clear inline comments (-- style) explaining each major step and any risk.
                                - Try to make the script idempotent: use IF NOT EXISTS / IF EXISTS where suitable.
                                - If a column or table must be removed, mark it as an explicit step with a safety note.
                                - If a data transformation is required, include an explicit data migration step and a brief risk explanation.

                                INPUT DIFF (unified):
                                ```diff
                                %s
                                ```

                                OLD DDL:
                                ```sql
                                %s
                                ```

                                NEW DDL:
                                ```sql
                                %s
                                ```

                                RESPONSE FORMAT (JSON):
                                {
                                    "confidence": number,                        // 0.0 .. 1.0
                                    "migrationScript": "string containing SQL", // SQL statements without code fences
                                    "comments": "string describing reasoning and risks"
                                }

                                EXAMPLE OUTPUT:
                                {
                                    "confidence": 0.88,
                                    "migrationScript": "-- Safe migration script\nBEGIN;\nALTER TABLE orders ...;\nCOMMIT;",
                                    "comments": "Changed id type and added customer name column. Applied safe ALTERs and preserved data."
                                }

                                IMPORTANT: The `migrationScript` MUST be only the SQL script as a string (no markdown fences), and `confidence` should be an approximate float between 0.0 and 1.0.
                                """.formatted(dialect.toUpperCase(), textDiff, oldDdl, newDdl);
    }
}
