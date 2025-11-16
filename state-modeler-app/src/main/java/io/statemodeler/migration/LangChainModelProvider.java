package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;

/**
 * Production implementation of {@link ChatModelProvider} using real LangChain4j models.
 *
 * <p>Supports Jlama (in-JVM) and Ollama (server-based) LLM providers.
 *
 * <p>This class isolates the dependency on concrete LangChain4j implementations, allowing the rest
 * of the codebase to depend only on the {@link ChatModelProvider} interface.
 */
public final class LangChainModelProvider implements ChatModelProvider {

    @Override
    public ChatLanguageModel createModel(String provider, String modelName, double temperature) {
        return switch (provider.toLowerCase()) {
            case "jlama" -> createJlamaModel(modelName, (float) temperature);
            case "ollama" -> createOllamaModel(modelName, temperature);
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }

    @Override
    public ChatLanguageModel createModel(
            String provider, String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        return switch (provider.toLowerCase()) {
            case "jlama" -> createJlamaModel(modelName, (float) temperature);
            case "ollama" -> createOllamaModel(baseUrl, modelName, temperature, timeoutSeconds);
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }

    private ChatLanguageModel createJlamaModel(String modelName, float temperature) {
        return JlamaChatModel.builder()
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    private ChatLanguageModel createOllamaModel(String modelName, double temperature) {
        return createOllamaModel("http://localhost:11434", modelName, temperature, 60);
    }

    private ChatLanguageModel createOllamaModel(
            String baseUrl, String modelName, double temperature, int timeoutSeconds) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
