package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Factory class for creating ChatLanguageModel instances.
 *
 * <p>Supports both Jlama (local in-JVM models) and Ollama (local server models).
 *
 * <p><b>Note:</b> This class is deprecated in favor of {@link ChatModelProvider} interface. Use
 * {@link LangChainModelProvider} for production or inject a custom provider for testing.
 *
 * @deprecated Use {@link ChatModelProvider} and {@link LangChainModelProvider} instead for better
 *     testability
 */
@Deprecated(since = "0.2", forRemoval = true)
public final class LangChainModelFactory {

    private static final ChatModelProvider DEFAULT_PROVIDER = new LangChainModelProvider();

    private LangChainModelFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a Jlama-based chat model (runs entirely in the JVM).
     *
     * <p>Example model names:
     *
     * <ul>
     *   <li>tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4
     *   <li>tjake/Llama-3.2-1B-Instruct-JQ4
     * </ul>
     *
     * @param modelName the HuggingFace model name (format: owner/model-name)
     * @param temperature creativity/randomness (0.0 = deterministic, 1.0 = very creative)
     * @return a configured JlamaChatModel
     * @deprecated Use {@link ChatModelProvider#createModel(String, String, double)} instead
     */
    @Deprecated(since = "0.2", forRemoval = true)
    public static ChatLanguageModel createJlamaModel(String modelName, Float temperature) {
        return DEFAULT_PROVIDER.createModel("jlama", modelName, temperature);
    }

    /**
     * Creates an Ollama-based chat model (requires Ollama server running).
     *
     * <p>Example model names:
     *
     * <ul>
     *   <li>llama3.2
     *   <li>mistral
     *   <li>codellama
     * </ul>
     *
     * <p>Make sure Ollama is running: {@code ollama serve} <br>
     * Pull a model first: {@code ollama pull llama3.2}
     *
     * @param baseUrl the Ollama server URL (default: http://localhost:11434)
     * @param modelName the Ollama model name
     * @param temperature creativity/randomness (0.0 = deterministic, 1.0 = very creative)
     * @param timeoutSeconds timeout for LLM requests
     * @return a configured OllamaChatModel
     * @deprecated Use {@link ChatModelProvider#createModel(String, String, double, String, int)}
     *     instead
     */
    @Deprecated(since = "0.2", forRemoval = true)
    public static ChatLanguageModel createOllamaModel(
            String baseUrl, String modelName, Double temperature, int timeoutSeconds) {
        return DEFAULT_PROVIDER.createModel("ollama", modelName, temperature, baseUrl, timeoutSeconds);
    }

    /**
     * Creates an Ollama model with default settings.
     *
     * @param modelName the Ollama model name (e.g., "llama3.2")
     * @return a configured OllamaChatModel with localhost:11434 and 60s timeout
     * @deprecated Use {@link ChatModelProvider#createModel(String, String, double)} instead
     */
    @Deprecated(since = "0.2", forRemoval = true)
    public static ChatLanguageModel createOllamaModel(String modelName) {
        return DEFAULT_PROVIDER.createModel("ollama", modelName, 0.2);
    }
}
