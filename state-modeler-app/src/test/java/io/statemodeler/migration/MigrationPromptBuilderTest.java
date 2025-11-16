package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MigrationPromptBuilderTest {

    @Test
    void shouldBuildValidPrompt() {
        // Given
        String oldDdl = "CREATE TABLE orders (id INT);";
        String newDdl = "CREATE TABLE orders (id BIGINT, name TEXT);";
        String textDiff = """
                --- old.sql
                +++ new.sql
                @@ -1 +1 @@
                -CREATE TABLE orders (id INT);
                +CREATE TABLE orders (id BIGINT, name TEXT);
                """;
        String dialect = "postgres";

        // When
        String prompt = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("POSTGRES"));
        assertTrue(prompt.contains("forward SQL migration"));
        assertTrue(prompt.contains(textDiff));
        assertTrue(prompt.contains(oldDdl));
        assertTrue(prompt.contains(newDdl));
        assertTrue(prompt.contains("ALTER TABLE"));
        assertTrue(prompt.contains("Avoid data loss"));
    }

    @Test
    void shouldThrowExceptionWhenOldDdlIsNull() {
        // Given
        String newDdl = "CREATE TABLE test (id INT);";
        String textDiff = "diff";
        String dialect = "postgres";

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MigrationPromptBuilder.buildPrompt(null, newDdl, textDiff, dialect));
        assertEquals("oldDdl cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNewDdlIsNull() {
        // Given
        String oldDdl = "CREATE TABLE test (id INT);";
        String textDiff = "diff";
        String dialect = "postgres";

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MigrationPromptBuilder.buildPrompt(oldDdl, null, textDiff, dialect));
        assertEquals("newDdl cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTextDiffIsNull() {
        // Given
        String oldDdl = "CREATE TABLE test (id INT);";
        String newDdl = "CREATE TABLE test (id BIGINT);";
        String dialect = "postgres";

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, null, dialect));
        assertEquals("textDiff cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDialectIsNull() {
        // Given
        String oldDdl = "CREATE TABLE test (id INT);";
        String newDdl = "CREATE TABLE test (id BIGINT);";
        String textDiff = "diff";

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, null));
        assertEquals("dialect cannot be null", exception.getMessage());
    }

    @Test
    void shouldIncludeDialectInUpperCase() {
        // Given
        String oldDdl = "CREATE TABLE test (id INT);";
        String newDdl = "CREATE TABLE test (id BIGINT);";
        String textDiff = "diff";
        String dialect = "mysql";

        // When
        String prompt = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertTrue(prompt.contains("MYSQL"));
    }
}
