package io.statemodeler.migration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Mock implementation of {@link ChatModelProvider} for testing.
 *
 * <p>Returns a deterministic mock ChatModel that generates predictable responses without
 * requiring actual LLM execution.
 *
 * <p>Example usage in tests:
 *
 * <pre>{@code
 * ChatModelProvider mockProvider = new MockChatModelProvider("-- Mock migration script");
 * ChatModel model = mockProvider.createModel("test-model", 0.0);
 * String response = model.chat("prompt");
 * assertEquals("-- Mock migration script", response);
 * }</pre>
 */
public final class MockChatModelProvider implements ChatModelProvider {

    private final String mockResponse;

    /**
     * Creates a mock provider with a fixed response.
     *
     * @param mockResponse the response to return for all chat() calls
     */
    public MockChatModelProvider(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    /** Creates a mock provider with default response. */
    public MockChatModelProvider() {
        this("-- Mock migration script\nBEGIN;\nCOMMIT;");
    }

    @Override
    public ChatModel createModel(String modelName, double temperature) {
        return new MockChatLanguageModel(mockResponse);
    }

    @Override
    public ChatModel createModel(String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        return new MockChatLanguageModel(mockResponse);
    }

    /**
     * Mock ChatModel implementation.
     *
     * <p>Returns the configured mock response for all chat() calls.
     */
    private static class MockChatLanguageModel implements ChatModel {
        private final String response;

        MockChatLanguageModel(String response) {
            this.response = response;
        }

        @Override
        public dev.langchain4j.model.chat.response.ChatResponse chat(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            return dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
        }

        @Override
        public String chat(String userMessage) {
            return response;
        }

        // chat(List<ChatMessage>) already implemented above
    }
}
