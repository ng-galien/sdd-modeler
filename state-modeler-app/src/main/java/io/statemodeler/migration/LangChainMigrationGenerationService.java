package io.statemodeler.migration;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.V;
import io.vavr.control.Try;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j-based implementation of {@link MigrationGenerationService}.
 *
 * <p>
 * Uses Ollama LLM with AiServices for structured outputs to generate SQL
 * migration scripts
 * from DDL differences. Returns a {@link MigrationResult} containing the
 * script, confidence,
 * and explanatory comments.
 */
public class LangChainMigrationGenerationService implements MigrationGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(LangChainMigrationGenerationService.class);

    private final MigrationAssistant assistant;

    /**
     * Creates the service with a configured LangChain4j chat model.
     *
     * @param chatModel the LLM to use for generation (must support structured
     *                  outputs)
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChainMigrationGenerationService(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel cannot be null");
        }
        this.assistant = AiServices.builder(MigrationAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Override
    public Try<MigrationResult> generateMigrationScript(String oldDdl, String newDdl, String textDiff, String dialect) {

        return Try.of(() -> {
            // Validate inputs
            if (oldDdl == null) {
                throw new IllegalArgumentException("oldDdl cannot be null");
            }
            if (newDdl == null) {
                throw new IllegalArgumentException("newDdl cannot be null");
            }
            if (textDiff == null) {
                throw new IllegalArgumentException("textDiff cannot be null");
            }
            if (dialect == null) {
                throw new IllegalArgumentException("dialect cannot be null");
            }

            logger.debug("Building migration prompt for dialect: {}", dialect);
            List<ChatMessage> messages = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

            logger.info("Calling LLM to generate migration script with structured output...");

            try {
                MigrationResult result = assistant.generateMigration(messages);
                logger.info(
                        "Migration generated successfully - confidence: {}, script length: {} chars",
                        result.confidence(),
                        result.migrationScript().length());
                return result;
            } catch (Exception e) {
                logger.error("Error during LLM call", e);
                // Rethrow original exception to preserve message for tests and callers
                throw e;
            }
        });
    }

    /**
     * AI service interface for migration generation.
     * LangChain4j automatically handles the structured output mapping.
     */
    interface MigrationAssistant {
        MigrationResult generateMigration(List<ChatMessage> messages);
    }
}
