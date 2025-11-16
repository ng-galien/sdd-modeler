package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Provider interface for creating ChatLanguageModel instances.
 *
 * <p>This abstraction allows dependency injection and testing without requiring actual LLM
 * implementations. Implementations can provide real models (Jlama, Ollama) or mock models for
 * testing.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Production: use real LLM provider
 * ChatModelProvider provider = new LangChainModelProvider();
 * ChatLanguageModel model = provider.createModel("jlama", "model-name", 0.7);
 *
 * // Testing: use mock provider
 * ChatModelProvider provider = new MockChatModelProvider();
 * ChatLanguageModel model = provider.createModel("mock", "test-model", 0.0);
 * }</pre>
 */
public interface ChatModelProvider {

    /**
     * Creates a ChatLanguageModel instance.
     *
     * @param provider LLM provider type ("jlama", "ollama", etc.)
     * @param modelName name of the model
     * @param temperature creativity parameter (0.0 = deterministic, 1.0 = creative)
     * @return configured ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unsupported
     */
    ChatLanguageModel createModel(String provider, String modelName, double temperature);

    /**
     * Creates a ChatLanguageModel with additional provider-specific configuration.
     *
     * @param provider LLM provider type
     * @param modelName name of the model
     * @param temperature creativity parameter
     * @param baseUrl base URL for remote models (e.g., Ollama server)
     * @param timeoutSeconds request timeout in seconds
     * @return configured ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unsupported
     */
    default ChatLanguageModel createModel(
            String provider, String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        // Default implementation delegates to simple createModel
        return createModel(provider, modelName, temperature);
    }
}
