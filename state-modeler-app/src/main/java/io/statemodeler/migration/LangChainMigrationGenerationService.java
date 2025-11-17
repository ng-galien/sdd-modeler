package io.statemodeler.migration;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j-based implementation of {@link MigrationGenerationService}.
 *
 * <p>Uses Ollama LLM with JSON Schema structured outputs to generate SQL migration scripts
 * from DDL differences. Returns a {@link MigrationResult} containing the script, confidence,
 * and explanatory comments.
 *
 * <p>Usage example:
 * <pre>
 * ChatModelProvider provider = new LangChainModelProvider();
 * var model = provider.createModel("llama3.2", 0.7);
 * var service = new LangChainMigrationGenerationService(model);
 *
 * // Generate migration with structured output
 * var result = service.generateMigrationScript(oldDdl, newDdl, diff, "postgres");
 * result.onSuccess(migrationResult -> {
 *     System.out.println("Confidence: " + migrationResult.confidence());
 *     System.out.println("Script: " + migrationResult.migrationScript());
 *     System.out.println("Comments: " + migrationResult.comments());
 * });
 * </pre>
 */
public class LangChainMigrationGenerationService implements MigrationGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(LangChainMigrationGenerationService.class);

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the service with a configured LangChain4j chat model.
     *
     * @param chatModel the LLM to use for generation (must support JSON Schema)
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChainMigrationGenerationService(ChatLanguageModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel cannot be null");
        }
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
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
            String prompt = MigrationPromptBuilder.buildPrompt(oldDdl, newDdl, textDiff, dialect);

            // Define JSON Schema for structured output
            JsonSchema jsonSchema = JsonSchema.builder()
                    .name("MigrationResult")
                    .rootElement(JsonObjectSchema.builder()
                            .addNumberProperty(
                                    "confidence",
                                    "LLM's confidence in the migration script (0.0 = no confidence, 1.0 = full confidence)")
                            .addStringProperty(
                                    "migrationScript",
                                    "SQL DDL migration script to transform from old schema to new schema")
                            .addStringProperty(
                                    "comments",
                                    "LLM's explanation and reasoning about the migration, including potential risks")
                            .required("confidence", "migrationScript", "comments")
                            .build())
                    .build();

            ResponseFormat responseFormat =
                    ResponseFormat.builder().type(JSON).jsonSchema(jsonSchema).build();

            logger.info("Calling LLM to generate migration script with structured output...");
            ChatRequest request = ChatRequest.builder()
                    .messages(UserMessage.from(prompt))
                    .responseFormat(responseFormat)
                    .build();

            ChatResponse response = chatModel.chat(request);
            String jsonOutput = response.aiMessage().text();

            if (jsonOutput == null || jsonOutput.isBlank()) {
                throw new IllegalStateException("LLM returned empty response");
            }

            logger.debug("LLM JSON output: {}", jsonOutput);

            // Parse JSON into MigrationResult
            MigrationResult migrationResult = objectMapper.readValue(jsonOutput, MigrationResult.class);

            logger.info(
                    "Migration generated successfully - confidence: {}, script length: {} chars",
                    migrationResult.confidence(),
                    migrationResult.migrationScript().length());

            return migrationResult;
        });
    }
}
