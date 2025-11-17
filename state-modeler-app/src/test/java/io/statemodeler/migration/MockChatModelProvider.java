package io.statemodeler.migration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

/**
 * Mock implementation of {@link ChatModelProvider} for testing.
 *
 * <p>Returns a deterministic mock ChatLanguageModel that generates predictable responses without
 * requiring actual LLM execution.
 *
 * <p>Example usage in tests:
 *
 * <pre>{@code
 * ChatModelProvider mockProvider = new MockChatModelProvider("-- Mock migration script");
 * ChatLanguageModel model = mockProvider.createModel("test-model", 0.0);
 * String response = model.generate("prompt");
 * assertEquals("-- Mock migration script", response);
 * }</pre>
 */
public final class MockChatModelProvider implements ChatModelProvider {

    private final String mockResponse;

    /**
     * Creates a mock provider with a fixed response.
     *
     * @param mockResponse the response to return for all generate() calls
     */
    public MockChatModelProvider(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    /** Creates a mock provider with default response. */
    public MockChatModelProvider() {
        this("-- Mock migration script\nBEGIN;\nCOMMIT;");
    }

    @Override
    public ChatLanguageModel createModel(String modelName, double temperature) {
        return new MockChatLanguageModel(mockResponse);
    }

    @Override
    public ChatLanguageModel createModel(String modelName, double temperature, String baseUrl, int timeoutSeconds) {
        return new MockChatLanguageModel(mockResponse);
    }

    /**
     * Mock ChatLanguageModel implementation.
     *
     * <p>Returns the configured mock response for all generate() calls.
     */
    private static class MockChatLanguageModel implements ChatLanguageModel {
        private final String response;

        MockChatLanguageModel(String response) {
            this.response = response;
        }

        @Override
        public Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            return Response.from(dev.langchain4j.data.message.AiMessage.from(response));
        }

        @Override
        public String generate(String userMessage) {
            return response;
        }
    }
}
