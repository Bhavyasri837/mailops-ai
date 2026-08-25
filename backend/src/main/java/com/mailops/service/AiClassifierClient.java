package com.mailops.service;

import com.mailops.dto.EmailClassificationRequest;
import com.mailops.exception.AIClassificationException;

/**
 * The thin boundary to whatever is actually producing classifications
 * (OpenAI, or the deterministic mock). Returns the raw JSON text the model
 * produced - AiClassificationService owns parsing and validation. Swapping
 * OPENAI vs MOCK never changes anything above this interface.
 */
public interface AiClassifierClient {

    String requestClassification(EmailClassificationRequest request) throws AIClassificationException;

    /**
     * Whether this client is a real AI call or a canned/deterministic
     * substitute. The frontend must be able to clearly show when mock mode
     * is active - never presented as a real AI result.
     */
    boolean isMock();
}
