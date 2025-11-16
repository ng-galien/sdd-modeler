package io.statemodeler.migration;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j-based implementation of {@link MigrationGenerationService}.
 *
 * <p>Uses a local LLM (via Jlama or Ollama) to generate SQL migration scripts
 * from DDL differences.
 *
 * <p>Usage example:
 * <pre>
 * // With Ollama
 * ChatModelProvider provider = new LangChainModelProvider();
 * var model = provider.createModel("ollama", "llama3.2", 0.7);
 * var service = new LangChainMigrationGenerationService(model);
 *
 * // With Jlama
 * var model = provider.createModel("jlama", "tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4", 0.2);
 * var service = new LangChainMigrationGenerationService(model);
 *
 * // Generate migration
 * var result = service.generateMigrationScript(oldDdl, newDdl, diff, "postgres");
 * result.onSuccess(script -> System.out.println(script));
 * result.onFailure(error -> System.err.println("Failed: " + error.getMessage()));
 * </pre>
 */
public class LangChainMigrationGenerationService implements MigrationGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(LangChainMigrationGenerationService.class);

    private final ChatLanguageModel chatModel;

    /**
     * Constructs the service with a configured LangChain4j chat model.
     *
     * @param chatModel the LLM to use for generation (Jlama or Ollama)
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChainMigrationGenerationService(ChatLanguageModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel cannot be null");
        }
        this.chatModel = chatModel;
    }

    @Override
    public Try<String> generateMigrationScript(String oldDdl, String newDdl, String textDiff, String dialect) {

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
            String prompt = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

            logger.info("Calling LLM to generate migration script...");
            ChatRequest request =
                    ChatRequest.builder().messages(UserMessage.from(prompt)).build();
            ChatResponse response = chatModel.chat(request);

            String migrationScript = response.aiMessage().text();

            if (migrationScript == null || migrationScript.isBlank()) {
                throw new IllegalStateException("LLM returned empty migration script");
            }

            logger.info("Migration script generated successfully ({} chars)", migrationScript.length());
            return migrationScript;
        });
    }
}
