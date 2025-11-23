package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;
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
        List<ChatMessage> messages = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertNotNull(messages);
        assertEquals(2, messages.size());

        ChatMessage systemMessage = messages.get(0);
        assertTrue(systemMessage instanceof SystemMessage);
        String systemText = ((SystemMessage) systemMessage).text();
        assertTrue(systemText.contains("POSTGRES"));
        assertTrue(systemText.contains("INSERT INTO"));
        assertTrue(systemText.contains("STRICTLY FORBIDDEN"));
        assertTrue(systemText.contains("TRANSACTION"));
        assertTrue(systemText.contains("TRIGGERS"));
        assertTrue(systemText.contains("single transaction"));

        ChatMessage userMessage = messages.get(1);
        assertTrue(userMessage instanceof UserMessage);
        String userText = userMessage.toString();
        assertTrue(userText.contains("copy data"));
        assertTrue(userText.contains(textDiff));
        assertTrue(userText.contains(oldDdl));
        assertTrue(userText.contains(newDdl));
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
        List<ChatMessage> messages = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

        // Then
        ChatMessage systemMessage = messages.get(0);
        assertTrue(((SystemMessage) systemMessage).text().contains("MYSQL"));
    }
}
