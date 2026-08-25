package com.mailops.service;

import com.mailops.dto.EmailClassificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * A deterministic, keyword-based substitute for the real LLM call, used only
 * when AI_MODE=MOCK. This exists purely so the application can be run and
 * demoed locally without an OpenAI API key - it is NEVER used as a stand-in
 * for the "real AI classification" requirement, and EmailDto/frontend must
 * always show a visible "MOCK MODE" indicator whenever this client is active
 * (see isMock()).
 *
 * The assessment demo must run with AI_MODE=OPENAI and a valid key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.mode", havingValue = "MOCK")
public class MockClassifierClient implements AiClassifierClient {

    @Override
    public String requestClassification(EmailClassificationRequest request) {
        log.warn("AI_MODE=MOCK active - returning a deterministic canned classification, NOT a real AI call.");
        String text = (safe(request.subject()) + " " + safe(request.body())).toLowerCase();

        if (containsAny(text, "won", "click here", "free", "urgent", "bitcoin", "% off", "claim your")) {
            return json("SPAM", 0.97,
                    "Contains classic unsolicited/promotional language.",
                    "{\"invoiceNumber\":null,\"amount\":null,\"vendor\":null,\"dueDate\":null}",
                    "{\"INVOICE_SUBMISSION\":0.0,\"PAYMENT_QUERY\":0.0,\"DISPUTE\":0.01,\"SPAM\":0.99}");
        }
        if (containsAny(text, "dispute", "disagree", "incorrect charge", "overcharge", "object")) {
            return json("DISPUTE", 0.9,
                    "Sender explicitly disputes an amount or charge.",
                    "{\"invoiceNumber\":null,\"amount\":null,\"vendor\":null,\"dueDate\":null}",
                    "{\"INVOICE_SUBMISSION\":0.03,\"PAYMENT_QUERY\":0.05,\"DISPUTE\":0.9,\"SPAM\":0.02}");
        }
        if (containsAny(text, "payment status", "when will", "has it been paid", "paid yet", "settled", "outstanding")) {
            return json("PAYMENT_QUERY", 0.88,
                    "Sender is asking about payment timing or status.",
                    "{\"invoiceNumber\":null,\"amount\":null,\"vendor\":null,\"dueDate\":null}",
                    "{\"INVOICE_SUBMISSION\":0.05,\"PAYMENT_QUERY\":0.88,\"DISPUTE\":0.05,\"SPAM\":0.02}");
        }
        if (containsAny(text, "invoice", "attached", "please process", "for payment")) {
            return json("INVOICE_SUBMISSION", 0.93,
                    "Sender is submitting an invoice for processing.",
                    "{\"invoiceNumber\":null,\"amount\":null,\"vendor\":null,\"dueDate\":null}",
                    "{\"INVOICE_SUBMISSION\":0.93,\"PAYMENT_QUERY\":0.04,\"DISPUTE\":0.02,\"SPAM\":0.01}");
        }

        return json("PAYMENT_QUERY", 0.55,
                "Ambiguous mock fallback - mentions an invoice but intent is unclear.",
                "{\"invoiceNumber\":null,\"amount\":null,\"vendor\":null,\"dueDate\":null}",
                "{\"INVOICE_SUBMISSION\":0.25,\"PAYMENT_QUERY\":0.55,\"DISPUTE\":0.15,\"SPAM\":0.05}");
    }

    @Override
    public boolean isMock() {
        return true;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String json(String intent, double confidence, String reason,
                         String extractedDataJson, String intentProbabilitiesJson) {
        return """
                {
                  "intent": "%s",
                  "confidence": %s,
                  "reason": "%s",
                  "evidence": ["Mock mode - heuristic keyword match, not a real AI decision."],
                  "extractedData": %s,
                  "intentProbabilities": %s
                }
                """.formatted(intent, confidence, reason, extractedDataJson, intentProbabilitiesJson);
    }
}
