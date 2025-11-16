package io.statemodeler.comparison;

import java.util.List;

/**
 * Immutable record representing a comparison between two DDL (Data Definition Language) scripts.
 *
 * <p>This record contains the current DDL, the future DDL, and a unified diff showing the
 * differences between them. The diff is generated using the java-diff-utils library.
 *
 * @param currentDdl The current DDL script (before changes)
 * @param futureDdl The future DDL script (after changes)
 * @param diff A list of strings representing the unified diff format (includes context lines, additions, deletions)
 */
public record DdlComparison(String currentDdl, String futureDdl, List<String> diff) {
    /**
     * Compact constructor that validates all fields are non-null.
     *
     * @throws IllegalArgumentException if any field is null
     */
    public DdlComparison {
        if (currentDdl == null) {
            throw new IllegalArgumentException("currentDdl cannot be null");
        }
        if (futureDdl == null) {
            throw new IllegalArgumentException("futureDdl cannot be null");
        }
        if (diff == null) {
            throw new IllegalArgumentException("diff cannot be null");
        }
        // Make the diff list immutable
        diff = List.copyOf(diff);
    }
}
