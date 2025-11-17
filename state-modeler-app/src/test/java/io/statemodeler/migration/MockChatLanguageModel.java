package io.statemodeler.migration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/** Simple reusable mock ChatModel for tests. */
public final class MockChatLanguageModel implements ChatModel {

    private final String response;

    public MockChatLanguageModel(String response) {
        this.response = response;
    }

    @Override
    public ChatResponse chat(java.util.List<ChatMessage> messages) {
        return ChatResponse.builder().aiMessage(AiMessage.from(response)).build();
    }

    @Override
    public String chat(String userMessage) {
        return response;
    }
}
