package io.statemodeler.migration;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;

/**
 * Helper class to build LLM prompts for SQL migration script generation.
 *
 * <p>
 * Constructs a detailed prompt containing:
 * <ul>
 * <li>System instructions (role, dialect, constraints)</li>
 * <li>Unified diff between old and new DDL</li>
 * <li>Complete old DDL</li>
 * <li>Complete new DDL</li>
 * </ul>
 */
final class MigrationPromptBuilder {

    private MigrationPromptBuilder() {
        // Utility class - no instantiation
    }

    /**
     * Builds the complete prompt for the LLM.
     *
     * @param oldDdl   the original DDL schema
     * @param newDdl   the target DDL schema
     * @param textDiff the unified diff
     * @param dialect  the SQL dialect (e.g., "postgres", "mysql")
     * @return the formatted prompt as a String
     */
    static List<ChatMessage> buildPrompt(String oldDdl, String newDdl, String textDiff, String dialect) {
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

        String systemPrompt = """
                SYSTEM: You are an expert database migration specialist. Reply only with a JSON object
                conforming to the described structure -- do NOT include extra text outside the JSON.

                CONSTRAINTS:
                - SQL Dialect: %s
                - STRICTLY FORBIDDEN: Do NOT delete, update, or drop any existing data or tables.
                - STRICTLY FORBIDDEN: Do NOT use ALTER TABLE to modify existing columns if it risks data loss.
                - GOAL: Create a script that migrates data using `INSERT INTO target_table (...) SELECT ... FROM source_table`.
                - TRANSACTION: The entire script MUST be wrapped in a single transaction (e.g., `BEGIN; ... COMMIT;`).
                - TRIGGERS: You MAY disable triggers at the start and re-enable them at the end if necessary to avoid side effects (e.g., `SET session_replication_role = 'replica';` for Postgres).
                - Assume the target tables (NEW DDL) might need to be created if they don't exist, or data inserted if they do.
                - If column names changed, map them correctly in the SELECT statement.
                - If types changed, cast them explicitly.
                - Add clear inline comments (-- style) explaining the data mapping.

                RESPONSE FORMAT (JSON):
                {
                    "confidence": number,                        // 0.0 .. 1.0
                    "migrationScript": "string containing SQL", // SQL statements without code fences
                    "comments": "string describing reasoning and risks"
                }

                EXAMPLE OUTPUT:
                {
                    "confidence": 0.95,
                    "migrationScript": "BEGIN;\\nSET session_replication_role = 'replica';\\n-- Copy data...\\nINSERT INTO new_orders (id, customer_name) SELECT id, CAST(name AS TEXT) FROM orders;\\nSET session_replication_role = 'origin';\\nCOMMIT;",
                    "comments": "Wrapped in transaction, disabled triggers, mapped 'name' to 'customer_name' and cast to TEXT."
                }

                IMPORTANT: The `migrationScript` MUST be only the SQL script as a string (no markdown fences), and `confidence` should be an approximate float between 0.0 and 1.0.
                """
                .formatted(dialect.toUpperCase());

        String userPrompt = """
                TASK: Generate a forward SQL migration script to copy data from the OLD schema structure to the NEW schema structure.

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
                """
                .formatted(textDiff, oldDdl, newDdl);

        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt));
    }
}
