package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for LangChainMigrationGenerationService.
 *
 * <p>NOTE: These tests use lambda mocks instead of real LLM calls.
 * To test with a real LLM, replace mockModel with an actual Ollama/Jlama instance.
 */
class LangChainMigrationGenerationServiceTest {

    @Test
    void shouldGenerateMigrationScript() {
        // Given
        String oldDdl = "CREATE TABLE orders (id INT);";
        String newDdl = "CREATE TABLE orders (id BIGINT, customer_name TEXT);";
        String textDiff = """
                --- old.sql
                +++ new.sql
                @@ -1 +1 @@
                -CREATE TABLE orders (id INT);
                +CREATE TABLE orders (id BIGINT, customer_name TEXT);
                """;
        String dialect = "postgres";

        String expectedScript = """
                -- Migration: Change id type and add customer_name column
                ALTER TABLE orders ALTER COLUMN id TYPE BIGINT;
                ALTER TABLE orders ADD COLUMN customer_name TEXT;
                """;

        // Mock ChatLanguageModel that returns JSON with MigrationResult structure
        String jsonResponse =
                String.format("""
                {
                  "confidence": 0.95,
                  "migrationScript": "%s",
                  "comments": "Changed id from INT to BIGINT and added customer_name column"
                }
                """, expectedScript.replace("\n", "\\n").replace("\"", "\\\""));

        ChatLanguageModel mockModel = createMockModel(jsonResponse);
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<MigrationResult> result = service.generateMigrationScript(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertTrue(result.isSuccess());
        MigrationResult migrationResult = result.get();
        assertEquals(0.95, migrationResult.confidence());
        assertEquals(expectedScript, migrationResult.migrationScript());
        assertEquals("Changed id from INT to BIGINT and added customer_name column", migrationResult.comments());
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("provideNullParameterCases")
    void shouldFailWhenParameterIsNull(
            String oldDdl, String newDdl, String textDiff, String dialect, String expectedMessage) {
        // Given
        ChatLanguageModel mockModel = createMockModel("test");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<MigrationResult> result = service.generateMigrationScript(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals(expectedMessage, cause.getMessage());
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
    void shouldFailWhenLlmReturnsEmptyScript() {
        // Given
        ChatLanguageModel mockModel = createMockModel("");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<MigrationResult> result = service.generateMigrationScript(
                "CREATE TABLE test (id INT);", "CREATE TABLE test (id BIGINT);", "diff", "postgres");

        // Then - empty string causes JSON parsing error or validation error
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        // Could be either JSON parsing exception or IllegalStateException for empty response
        assertNotNull(cause);
    }

    @Test
    void shouldFailWhenLlmThrowsException() {
        // Given
        ChatLanguageModel mockModel = new ChatLanguageModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                throw new RuntimeException("LLM connection failed");
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                throw new RuntimeException("LLM connection failed");
            }
        };

        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<MigrationResult> result = service.generateMigrationScript(
                "CREATE TABLE test (id INT);", "CREATE TABLE test (id BIGINT);", "diff", "postgres");

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof RuntimeException);
        assertEquals("LLM connection failed", cause.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenChatModelIsNull() {
        // When/Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new LangChainMigrationGenerationService(null));
        assertEquals("chatModel cannot be null", exception.getMessage());
    }

    // Helper method to create a simple mock ChatLanguageModel
    private ChatLanguageModel createMockModel(String response) {
        return new ChatLanguageModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(response))
                        .build();
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                return Response.from(AiMessage.from(response));
            }
        };
    }
}
