package com.mailops.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the official OpenAI Java SDK client as a Spring bean. Only created
 * when ai.mode=OPENAI, so local development in MOCK mode never requires an
 * API key to be present.
 *
 * NOTE: the OpenAI Java SDK's builder surface can change between releases.
 * This is written against the openai-java 0.31.x client builder pattern
 * (OpenAIOkHttpClient.builder().apiKey(...).build()). If the pinned version
 * in pom.xml differs, check com.openai.client.okhttp.OpenAIOkHttpClient's
 * builder methods and adjust this one method accordingly - nothing else in
 * the codebase depends on the SDK directly except OpenAiClassifierClient.
 */
@Configuration
@ConditionalOnProperty(name = "ai.mode", havingValue = "OPENAI", matchIfMissing = true)
public class OpenAiConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI_MODE=OPENAI but OPENAI_API_KEY is not set. " +
                    "Set OPENAI_API_KEY, or set AI_MODE=MOCK for local development without a key.");
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
