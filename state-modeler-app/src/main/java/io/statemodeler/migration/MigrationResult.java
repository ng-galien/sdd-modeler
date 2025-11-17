package io.statemodeler.migration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured output from LLM-based migration generation.
 *
 * <p>This record captures the LLM's migration script along with metadata about confidence and
 * reasoning. Used with LangChain4j's JSON Schema structured outputs feature.
 *
 * <p>Example JSON output from LLM:
 *
 * <pre>{@code
 * {
 *   "confidence": 0.95,
 *   "migrationScript": "ALTER TABLE orders ADD COLUMN status VARCHAR(50);",
 *   "comments": "Added status column to track order state. High confidence as the change is straightforward."
 * }
 * }</pre>
 *
 * @param confidence LLM's confidence in the migration script (0.0 = no confidence, 1.0 = full
 *     confidence)
 * @param migrationScript SQL DDL migration script to transform from old schema to new schema
 * @param comments LLM's explanation and reasoning about the migration, including potential risks or
 *     considerations
 */
public record MigrationResult(
        @JsonProperty(required = true) double confidence,
        @JsonProperty(required = true) String migrationScript,
        @JsonProperty(required = true) String comments) {

    /**
     * Validates the migration result.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public MigrationResult {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0, got: " + confidence);
        }
        if (migrationScript == null || migrationScript.isBlank()) {
            throw new IllegalArgumentException("migrationScript cannot be null or blank");
        }
        if (comments == null || comments.isBlank()) {
            throw new IllegalArgumentException("comments cannot be null or blank");
        }
    }
}
