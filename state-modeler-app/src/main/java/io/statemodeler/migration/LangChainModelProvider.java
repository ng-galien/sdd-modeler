package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;

/**
 * Production implementation of {@link ChatModelProvider} using Ollama.
 *
 * <p>Supports only Ollama (server-based) LLM provider for simplicity.
 *
 * <p>This class isolates the dependency on Ollama, allowing the rest of the codebase to depend
 * only on the {@link ChatModelProvider} interface for testability.
 *
 * <h2>Prerequisites</h2>
 *
 * <ul>
 *   <li>Ollama server running (default: http://localhost:11434)
 *   <li>Model pulled (e.g., {@code ollama pull llama3.2})
 * </ul>
 */
public final class LangChainModelProvider implements ChatModelProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public ChatModel createModel(String modelName, double temperature) {
        return createOllamaModel(modelName, temperature);
    }

    @Override
    public ChatModel createModel(String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        return createOllamaModel(baseUrl, modelName, temperature, timeoutSeconds);
    }

    private ChatModel createOllamaModel(String modelName, double temperature) {
        return createOllamaModel(DEFAULT_BASE_URL, modelName, temperature, DEFAULT_TIMEOUT_SECONDS);
    }

    private ChatModel createOllamaModel(String baseUrl, String modelName, double temperature, int timeoutSeconds) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
