package com.mailops.dto;

import java.util.List;
import java.util.Map;

/**
 * Raw, not-yet-validated shape of what the model returns. Deserialized
 * directly from the OpenAI JSON response by Jackson. AiClassificationService
 * is responsible for validating every field before this is trusted anywhere
 * else in the system - nothing downstream should assume this is well-formed.
 *
 * intentProbabilities is a probability-per-intent breakdown across all four
 * intents (e.g. {"PAYMENT_QUERY": 0.61, "DISPUTE": 0.27, "INVOICE_SUBMISSION": 0.12,
 * "SPAM": 0.0}), requested directly from the model in the same structured
 * response as "intent"/"confidence" - it is genuine model output, not
 * fabricated after the fact. Used to render the ambiguous-email breakdown UI.
 */
public record ClassificationResult(
        String intent,
        Double confidence,
        String reason,
        List<String> evidence,
        ExtractedEmailData extractedData,
        Map<String, Double> intentProbabilities
) {
}
