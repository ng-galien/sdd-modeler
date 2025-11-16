package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;

/**
 * Factory class for creating ChatLanguageModel instances.
 *
 * <p>Supports both Jlama (local in-JVM models) and Ollama (local server models).
 */
public final class LangChainModelFactory {

    private LangChainModelFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a Jlama-based chat model (runs entirely in the JVM).
     *
     * <p>Example model names:
     * <ul>
     *   <li>tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4</li>
     *   <li>tjake/Llama-3.2-1B-Instruct-JQ4</li>
     * </ul>
     *
     * @param modelName the HuggingFace model name (format: owner/model-name)
     * @param temperature creativity/randomness (0.0 = deterministic, 1.0 = very creative)
     * @return a configured JlamaChatModel
     */
    public static ChatLanguageModel createJlamaModel(String modelName, Float temperature) {
        // TODO: Configure modelCachePath if needed (default: ~/.jlama)
        // TODO: Configure workingDirectory for persistent chat memory if needed
        // TODO: Set quantizeModelAtRuntime to true if you want runtime quantization

        return JlamaChatModel.builder()
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    /**
     * Creates an Ollama-based chat model (requires Ollama server running).
     *
     * <p>Example model names:
     * <ul>
     *   <li>llama3.2</li>
     *   <li>mistral</li>
     *   <li>codellama</li>
     * </ul>
     *
     * <p>Make sure Ollama is running: {@code ollama serve}
     * <br>Pull a model first: {@code ollama pull llama3.2}
     *
     * @param baseUrl the Ollama server URL (default: http://localhost:11434)
     * @param modelName the Ollama model name
     * @param temperature creativity/randomness (0.0 = deterministic, 1.0 = very creative)
     * @param timeoutSeconds timeout for LLM requests
     * @return a configured OllamaChatModel
     */
    public static ChatLanguageModel createOllamaModel(
            String baseUrl, String modelName, Double temperature, int timeoutSeconds) {
        // TODO: Customize other parameters if needed (topK, topP, repeatPenalty, etc.)
        // TODO: Set logRequests(true) and logResponses(true) for debugging

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Creates an Ollama model with default settings.
     *
     * @param modelName the Ollama model name (e.g., "llama3.2")
     * @return a configured OllamaChatModel with localhost:11434 and 60s timeout
     */
    public static ChatLanguageModel createOllamaModel(String modelName) {
        return createOllamaModel("http://localhost:11434", modelName, 0.2, 60);
    }
}
