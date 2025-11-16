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
import org.junit.jupiter.api.Test;

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

        // Mock ChatLanguageModel that returns a predefined response
        ChatLanguageModel mockModel = createMockModel(expectedScript);
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript(oldDdl, newDdl, textDiff, dialect);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(expectedScript, result.get());
    }

    @Test
    void shouldFailWhenOldDdlIsNull() {
        // Given
        ChatLanguageModel mockModel = createMockModel("test");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript(null, "CREATE TABLE test (id INT);", "diff", "postgres");

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("oldDdl cannot be null", cause.getMessage());
    }

    @Test
    void shouldFailWhenNewDdlIsNull() {
        // Given
        ChatLanguageModel mockModel = createMockModel("test");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript("CREATE TABLE test (id INT);", null, "diff", "postgres");

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("newDdl cannot be null", cause.getMessage());
    }

    @Test
    void shouldFailWhenTextDiffIsNull() {
        // Given
        ChatLanguageModel mockModel = createMockModel("test");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript(
                "CREATE TABLE test (id INT);", "CREATE TABLE test (id BIGINT);", null, "postgres");

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("textDiff cannot be null", cause.getMessage());
    }

    @Test
    void shouldFailWhenDialectIsNull() {
        // Given
        ChatLanguageModel mockModel = createMockModel("test");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript(
                "CREATE TABLE test (id INT);", "CREATE TABLE test (id BIGINT);", "diff", null);

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("dialect cannot be null", cause.getMessage());
    }

    @Test
    void shouldFailWhenLlmReturnsEmptyScript() {
        // Given
        ChatLanguageModel mockModel = createMockModel("");
        var service = new LangChainMigrationGenerationService(mockModel);

        // When
        Try<String> result = service.generateMigrationScript(
                "CREATE TABLE test (id INT);", "CREATE TABLE test (id BIGINT);", "diff", "postgres");

        // Then
        assertTrue(result.isFailure());
        Throwable cause = result.getCause();
        assertTrue(cause instanceof IllegalStateException);
        assertEquals("LLM returned empty migration script", cause.getMessage());
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
        Try<String> result = service.generateMigrationScript(
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
