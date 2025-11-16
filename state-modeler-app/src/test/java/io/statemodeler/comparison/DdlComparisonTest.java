package io.statemodeler.comparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DdlComparisonTest {

    @Test
    void shouldCreateValidDdlComparison() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id BIGINT);";
        List<String> diff = List.of(
                "--- current.sql",
                "+++ future.sql",
                "@@ -1 +1 @@",
                "-CREATE TABLE test (id INT);",
                "+CREATE TABLE test (id BIGINT);");

        // When
        DdlComparison comparison = new DdlComparison(currentDdl, futureDdl, diff);

        // Then
        assertEquals(currentDdl, comparison.currentDdl());
        assertEquals(futureDdl, comparison.futureDdl());
        assertEquals(diff, comparison.diff());
    }

    @Test
    void shouldThrowExceptionWhenCurrentDdlIsNull() {
        // Given
        String futureDdl = "CREATE TABLE test (id BIGINT);";
        List<String> diff = List.of();

        // When/Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new DdlComparison(null, futureDdl, diff));
        assertEquals("currentDdl cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFutureDdlIsNull() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        List<String> diff = List.of();

        // When/Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new DdlComparison(currentDdl, null, diff));
        assertEquals("futureDdl cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDiffIsNull() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id BIGINT);";

        // When/Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new DdlComparison(currentDdl, futureDdl, null));
        assertEquals("diff cannot be null", exception.getMessage());
    }

    @Test
    void shouldMakeDiffListImmutable() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id BIGINT);";
        List<String> mutableDiff = new java.util.ArrayList<>(List.of("--- current.sql", "+++ future.sql"));

        // When
        DdlComparison comparison = new DdlComparison(currentDdl, futureDdl, mutableDiff);

        // Then - the diff should be immutable
        assertThrows(
                UnsupportedOperationException.class, () -> comparison.diff().add("new line"));
    }

    @Test
    void shouldAllowEmptyDdlStrings() {
        // Given
        String currentDdl = "";
        String futureDdl = "";
        List<String> diff = List.of();

        // When
        DdlComparison comparison = new DdlComparison(currentDdl, futureDdl, diff);

        // Then
        assertTrue(comparison.currentDdl().isEmpty());
        assertTrue(comparison.futureDdl().isEmpty());
        assertTrue(comparison.diff().isEmpty());
    }

    @Test
    void shouldAllowEmptyDiffList() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id INT);"; // Identical
        List<String> diff = List.of(); // No differences

        // When
        DdlComparison comparison = new DdlComparison(currentDdl, futureDdl, diff);

        // Then
        assertTrue(comparison.diff().isEmpty());
    }

    @Test
    void shouldSupportRecordEquality() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id BIGINT);";
        List<String> diff = List.of("--- current.sql", "+++ future.sql");

        // When
        DdlComparison comparison1 = new DdlComparison(currentDdl, futureDdl, diff);
        DdlComparison comparison2 = new DdlComparison(currentDdl, futureDdl, diff);

        // Then
        assertEquals(comparison1, comparison2);
        assertEquals(comparison1.hashCode(), comparison2.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        // Given
        String currentDdl = "CREATE TABLE test (id INT);";
        String futureDdl = "CREATE TABLE test (id BIGINT);";
        List<String> diff = List.of("diff line");

        // When
        DdlComparison comparison = new DdlComparison(currentDdl, futureDdl, diff);
        String toString = comparison.toString();

        // Then
        assertTrue(toString.contains("DdlComparison"));
        assertTrue(toString.contains(currentDdl));
        assertTrue(toString.contains(futureDdl));
        assertTrue(toString.contains("diff line"));
    }
}
