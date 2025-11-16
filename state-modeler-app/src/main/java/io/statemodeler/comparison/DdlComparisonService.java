package io.statemodeler.comparison;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import java.util.Arrays;
import java.util.List;

/**
 * Service for comparing DDL (Data Definition Language) scripts and generating unified diff output.
 *
 * <p>This service uses the java-diff-utils library to compute differences between two DDL strings
 * and generate a unified diff format that can be used for visualization or migration purposes.
 *
 * <p>Example usage:
 * <pre>{@code
 * DdlComparisonService service = new DdlComparisonService();
 * DdlComparison comparison = service.compare(currentDdl, futureDdl);
 * comparison.diff().forEach(System.out::println);
 * }</pre>
 */
public final class DdlComparisonService {

    private static final int UNIFIED_DIFF_CONTEXT_SIZE = 3; // Number of context lines before/after changes

    /**
     * Compares two DDL scripts and generates a unified diff.
     *
     * <p>The unified diff format includes:
     * <ul>
     *   <li>Header lines showing file names and timestamps</li>
     *   <li>Hunk headers (@@) showing line number ranges</li>
     *   <li>Context lines (unchanged, prefixed with space)</li>
     *   <li>Deletion lines (prefixed with '-')</li>
     *   <li>Addition lines (prefixed with '+')</li>
     * </ul>
     *
     * @param currentDdl The current DDL script (before changes)
     * @param futureDdl The future DDL script (after changes)
     * @return A {@link DdlComparison} record containing both DDLs and their unified diff
     * @throws IllegalArgumentException if either DDL is null
     */
    public DdlComparison compare(String currentDdl, String futureDdl) {
        if (currentDdl == null) {
            throw new IllegalArgumentException("currentDdl cannot be null");
        }
        if (futureDdl == null) {
            throw new IllegalArgumentException("futureDdl cannot be null");
        }

        // Split DDL strings into lines
        List<String> currentLines = splitIntoLines(currentDdl);
        List<String> futureLines = splitIntoLines(futureDdl);

        // Compute the diff patch
        Patch<String> patch = DiffUtils.diff(currentLines, futureLines);

        // Generate unified diff format
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                "current.sql", // Original file name
                "future.sql", // Revised file name
                currentLines, // Original lines
                patch, // Computed patch
                UNIFIED_DIFF_CONTEXT_SIZE // Context size
                );

        return new DdlComparison(currentDdl, futureDdl, unifiedDiff);
    }

    /**
     * Splits a string into lines, handling different line ending styles (LF, CRLF).
     *
     * <p>Empty strings result in an empty list rather than a list with one empty string.
     *
     * @param text The text to split into lines
     * @return A list of lines (without line terminators)
     */
    private List<String> splitIntoLines(String text) {
        if (text.isEmpty()) {
            return List.of();
        }
        // Split by any line separator (handles \n, \r\n, \r)
        return Arrays.asList(text.split("\\r?\\n"));
    }
}
