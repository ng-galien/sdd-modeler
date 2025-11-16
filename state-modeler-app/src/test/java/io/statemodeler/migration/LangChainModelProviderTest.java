package io.statemodeler.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LangChainModelProvider}.
 *
 * <p>Note: These tests verify provider logic without executing actual LLM models. Real model
 * creation requires LangChain4j dependencies which may not be available in all test environments.
 */
class LangChainModelProviderTest {

    @Test
    void shouldRejectUnsupportedProvider() {
        // Given
        var provider = new LangChainModelProvider();

        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> provider.createModel("unsupported", "model-name", 0.5));

        assertTrue(exception.getMessage().contains("Unsupported LLM provider"));
        assertTrue(exception.getMessage().contains("unsupported"));
    }

    @Test
    void shouldRejectUnsupportedProviderWithFullParameters() {
        // Given
        var provider = new LangChainModelProvider();

        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.createModel("unknown", "model-name", 0.5, "http://localhost:8080", 60));

        assertTrue(exception.getMessage().contains("Unsupported LLM provider"));
    }

    // Note: Actual model creation tests for "jlama" and "ollama" are disabled
    // because they require LangChain4j runtime dependencies (Jlama, Ollama libraries)
    // which cause NoClassDefFoundError in test environments without those JARs.
    //
    // The provider is tested indirectly through:
    // 1. LangChainMigrationGenerationServiceTest (uses mock ChatLanguageModel)
    // 2. Integration tests that actually execute migrations (optional, requires downloads)
    //
    // Example disabled test:
    // @Test
    // void shouldCreateJlamaModel() {
    //     var provider = new LangChainModelProvider();
    //     var model = provider.createModel("jlama", "model-name", 0.7);
    //     assertNotNull(model); // Requires langchain4j-jlama JAR at runtime
    // }
}
