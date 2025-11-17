package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LangChainModelProvider}.
 *
 * <p>Note: These tests verify basic provider logic. The provider parameter is ignored since only
 * Ollama is supported. Actual model creation tests require Ollama server running and are better
 * suited for integration tests.
 */
class LangChainModelProviderTest {

    @Test
    void shouldCreateOllamaModel() {
        // Given - only Ollama supported, model name determines behavior
        var provider = new LangChainModelProvider();

        // When/Then - no exception thrown
        assertDoesNotThrow(() -> {
            // Note: This will create OllamaChatModel object but won't connect to server
            var model = provider.createModel("llama3.2", 0.5);
            assertNotNull(model);
        });
    }

    @Test
    void shouldCreateOllamaModelWithFullParameters() {
        // Given
        var provider = new LangChainModelProvider();

        // When/Then - creates Ollama model with custom baseUrl and timeout
        assertDoesNotThrow(() -> {
            var model = provider.createModel("llama3.2", 0.5, "http://localhost:11434", 60);
            assertNotNull(model);
        });
    }

    // Note: Actual model execution tests (chat, streaming) are disabled
    // because they require:
    // 1. Ollama server running (ollama serve)
    // 2. Model downloaded (ollama pull llama3.2)
    //
    // The provider is tested indirectly through:
    // 1. LangChainMigrationGenerationServiceTest (uses mock ChatLanguageModel)
    // 2. Integration tests with real Ollama server (optional)
    // void shouldCreateJlamaModel() {
    //     var provider = new LangChainModelProvider();
    //     var model = provider.createModel("jlama", "model-name", 0.7);
    //     assertNotNull(model); // Requires langchain4j-jlama JAR at runtime
    // }
}
