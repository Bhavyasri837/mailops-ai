package com.mailops.service;
import com.mailops.dto.EmailClassificationRequest;
import com.mailops.exception.AIClassificationException;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonObject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Calls the real OpenAI API via the official Java SDK. This is the ONLY
 * class in the codebase that talks to the OpenAI SDK directly - everything
 * else depends on the AiClassifierClient interface.
 *
 * NOTE ON SDK SURFACE: written against openai-java's
 * ChatCompletionCreateParams builder (addSystemMessage/addUserMessage,
 * responseFormat as JSON object, model via ChatModel). SDK method names can
 * shift between versions - if this fails to compile against the exact
 * version resolved from pom.xml, check the current ChatCompletionCreateParams
 * builder methods for setting response_format to "json_object"; the prompt
 * text and validation logic elsewhere do not need to change.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.mode", havingValue = "OPENAI", matchIfMissing = true)
public class OpenAiClassifierClient implements AiClassifierClient {

    private final OpenAIClient client;
    private final ClassificationPromptBuilder promptBuilder;
    private final String model;

    public OpenAiClassifierClient(
            OpenAIClient client,
            ClassificationPromptBuilder promptBuilder,
            @Value("${ai.openai.model}") String model
    ) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.model = model;
    }

    @Override
    public String requestClassification(EmailClassificationRequest request) {
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(model))
                    .addSystemMessage(promptBuilder.buildSystemPrompt())
                    .addUserMessage(promptBuilder.buildUserPrompt(request))
                    .responseFormat(ResponseFormatJsonObject.builder().build())

                    .temperature(0.0)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);

            String content = completion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse(null);

            if (content == null || content.isBlank()) {
                throw new AIClassificationException("OpenAI returned an empty response.");
            }
            return content;

        } catch (AIClassificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI classification call failed", e);
            throw new AIClassificationException("OpenAI classification call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isMock() {
        return false;
    }
}
