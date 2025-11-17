package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideNullParameterCases")
    void shouldThrowExceptionWhenParameterIsNull(
            String oldDdl, String newDdl, String textDiff, String dialect, String expectedMessage) {
        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect));
        assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> provideNullParameterCases() {
        String validDdl = "CREATE TABLE test (id INT);";
        String validDiff = "diff";
        String validDialect = "postgres";

        return Stream.of(
                Arguments.of(null, validDdl, validDiff, validDialect, "oldDdl cannot be null"),
                Arguments.of(validDdl, null, validDiff, validDialect, "newDdl cannot be null"),
                Arguments.of(validDdl, validDdl, null, validDialect, "textDiff cannot be null"),
                Arguments.of(validDdl, validDdl, validDiff, null, "dialect cannot be null"));
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
