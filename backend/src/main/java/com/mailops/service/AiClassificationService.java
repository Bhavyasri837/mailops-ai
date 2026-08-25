package com.mailops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailops.dto.ClassificationResult;
import com.mailops.dto.EmailClassificationRequest;
import com.mailops.dto.ExtractedEmailData;
import com.mailops.entity.Intent;
import com.mailops.exception.AIClassificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates classification: builds nothing itself (that's
 * ClassificationPromptBuilder's job), delegates the actual call to whichever
 * AiClassifierClient is active, then rigorously validates the response
 * before returning it. This class NEVER writes to the database, never
 * changes an Email's status, and never decides whether an action should run
 * - that is EmailProcessingService's job, using ConfidenceService.
 *
 * The core rule enforced here: the LLM's output is never trusted blindly.
 * Any structurally invalid response is treated as a classification failure
 * (AIClassificationException), which the caller routes to NEEDS_REVIEW -
 * never silently "fixed up" and passed off as a confident result.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiClassifierClient client;
    private final ObjectMapper objectMapper;

    public ClassificationResult classify(EmailClassificationRequest request) {
        String rawJson = client.requestClassification(request);
        ClassificationResult parsed = parse(rawJson);
        return validate(parsed);
    }

    public boolean isMockMode() {
        return client.isMock();
    }

    private ClassificationResult parse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFences(rawJson));

            String intent = textOrNull(root, "intent");
            Double confidence = root.hasNonNull("confidence") ? root.get("confidence").asDouble() : null;
            String reason = textOrNull(root, "reason");

            List<String> evidence = new ArrayList<>();
            if (root.has("evidence") && root.get("evidence").isArray()) {
                root.get("evidence").forEach(n -> evidence.add(n.asText()));
            }

            ExtractedEmailData extractedData = ExtractedEmailData.empty();
            if (root.has("extractedData") && root.get("extractedData").isObject()) {
                JsonNode ed = root.get("extractedData");
                extractedData = new ExtractedEmailData(
                        textOrNull(ed, "invoiceNumber"),
                        textOrNull(ed, "amount"),
                        textOrNull(ed, "vendor"),
                        textOrNull(ed, "dueDate")
                );
            }

            Map<String, Double> probabilities = new LinkedHashMap<>();
            if (root.has("intentProbabilities") && root.get("intentProbabilities").isObject()) {
                root.get("intentProbabilities").fields().forEachRemaining(
                        e -> probabilities.put(e.getKey(), e.getValue().asDouble()));
            }

            return new ClassificationResult(intent, confidence, reason, evidence, extractedData, probabilities);

        } catch (Exception e) {
            log.error("Failed to parse AI classification response. Raw response: {}", rawJson, e);
            throw new AIClassificationException("AI response was not valid JSON in the expected shape.", e);
        }
    }

    /**
     * Hard validation gate. Anything that doesn't pass this throws
     * AIClassificationException - there is no "best effort" repair of a
     * structurally broken or nonsensical classification.
     */
    private ClassificationResult validate(ClassificationResult result) {
        if (result.intent() == null) {
            throw new AIClassificationException("AI response missing 'intent'.");
        }

        String normalizedIntent = result.intent().trim().toUpperCase();
        boolean validIntent = Arrays.stream(Intent.values())
                .anyMatch(i -> i.name().equals(normalizedIntent));
        if (!validIntent) {
            throw new AIClassificationException(
                    "AI returned an unrecognized intent: '" + result.intent() + "'.");
        }

        if (result.confidence() == null || result.confidence() < 0.0 || result.confidence() > 1.0) {
            throw new AIClassificationException(
                    "AI returned an invalid confidence value: " + result.confidence());
        }

        List<String> evidence = result.evidence() == null ? List.of() : result.evidence();
        String reason = result.reason() == null ? "" : result.reason();
        ExtractedEmailData extractedData = result.extractedData() == null
                ? ExtractedEmailData.empty() : result.extractedData();
        Map<String, Double> probabilities = normalizeProbabilities(result.intentProbabilities(), normalizedIntent, result.confidence());

        return new ClassificationResult(normalizedIntent, result.confidence(), reason, evidence, extractedData, probabilities);
    }

    /**
     * Ensures the UI always has a full four-key breakdown to render, even if
     * the model omitted some entries. Missing keys are filled from the
     * primary intent/confidence rather than fabricated as precise numbers.
     */
    private Map<String, Double> normalizeProbabilities(Map<String, Double> given, String primaryIntent, double confidence) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Intent intent : Intent.values()) {
            if (given != null && given.containsKey(intent.name())) {
                result.put(intent.name(), given.get(intent.name()));
            } else if (intent.name().equals(primaryIntent)) {
                result.put(intent.name(), confidence);
            } else {
                result.put(intent.name(), 0.0);
            }
        }
        return result;
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
