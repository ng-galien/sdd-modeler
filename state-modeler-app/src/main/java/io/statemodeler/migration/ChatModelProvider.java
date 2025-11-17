package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Provider interface for creating ChatModel instances.
 *
 * <p>This abstraction allows dependency injection and testing without requiring actual LLM
 * implementations. Implementations can provide real Ollama models or mock models for testing.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Production: use real Ollama provider
 * ChatModelProvider provider = new LangChainModelProvider();
 * ChatModel model = provider.createModel("llama3.2", 0.7);
 *
 * // Testing: use mock provider
 * ChatModelProvider provider = new MockChatModelProvider();
 * ChatModel model = provider.createModel("test-model", 0.0);
 * }</pre>
 */
public interface ChatModelProvider {

    /**
     * Creates a ChatModel instance.
     *
     * @param modelName name of the model (e.g., "llama3.2", "mistral")
     * @param temperature creativity parameter (0.0 = deterministic, 1.0 = creative)
     * @return configured ChatModel instance
     */
    ChatModel createModel(String modelName, double temperature);

    /**
     * Creates a ChatModel with additional configuration.
     *
     * @param modelName name of the model
     * @param temperature creativity parameter
     * @param baseUrl base URL for Ollama server (e.g., "http://localhost:11434")
     * @param timeoutSeconds request timeout in seconds
     * @return configured ChatModel instance
     */
    default ChatModel createModel(String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        // Default implementation delegates to simple createModel
        return createModel(modelName, temperature);
    }
}
