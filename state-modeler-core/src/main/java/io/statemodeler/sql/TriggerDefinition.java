package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Represents a SQL trigger definition.
 *
 * <p>
 * This is a dialect-agnostic representation that can be rendered to specific
 * SQL dialects.
 *
 * @param name         trigger name
 * @param table        fully qualified table name (schema.table) where the
 *                     trigger will be created
 * @param timing       trigger timing - "BEFORE" or "AFTER"
 * @param events       list of trigger events (e.g., "INSERT", "UPDATE",
 *                     "DELETE")
 * @param forEach      trigger granularity - "ROW" or "STATEMENT"
 * @param functionName fully qualified function name (schema.function) to
 *                     execute
 * @param functionArgs arguments to pass to the trigger function
 */
public record TriggerDefinition(
        String name,
        String table,
        String timing,
        List<String> events,
        String forEach,
        String functionName,
        List<String> functionArgs) {

    public TriggerDefinition {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        Objects.requireNonNull(table, "table cannot be null");
        if (table.isBlank()) {
            throw new IllegalArgumentException("table cannot be blank");
        }

        Objects.requireNonNull(timing, "timing cannot be null");
        if (!timing.equals("BEFORE") && !timing.equals("AFTER")) {
            throw new IllegalArgumentException("timing must be 'BEFORE' or 'AFTER', got: " + timing);
        }

        events = List.copyOf(Objects.requireNonNull(events, "events cannot be null"));
        if (events.isEmpty()) {
            throw new IllegalArgumentException("events cannot be empty");
        }
        for (var event : events) {
            if (!event.equals("INSERT") && !event.equals("UPDATE") && !event.equals("DELETE")) {
                throw new IllegalArgumentException("event must be 'INSERT', 'UPDATE', or 'DELETE', got: " + event);
            }
        }

        Objects.requireNonNull(forEach, "forEach cannot be null");
        if (!forEach.equals("ROW") && !forEach.equals("STATEMENT")) {
            throw new IllegalArgumentException("forEach must be 'ROW' or 'STATEMENT', got: " + forEach);
        }

        Objects.requireNonNull(functionName, "functionName cannot be null");
        if (functionName.isBlank()) {
            throw new IllegalArgumentException("functionName cannot be blank");
        }

        functionArgs = List.copyOf(Objects.requireNonNull(functionArgs, "functionArgs cannot be null"));
    }
}
