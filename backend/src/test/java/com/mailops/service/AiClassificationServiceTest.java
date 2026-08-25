package com.mailops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailops.dto.ClassificationResult;
import com.mailops.dto.EmailClassificationRequest;
import com.mailops.exception.AIClassificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AiClassificationServiceTest {

    @Mock
    private AiClassifierClient client;

    private AiClassificationService service;
    private final EmailClassificationRequest request =
            new EmailClassificationRequest("a@b.com", "Invoice", "Please pay invoice INV-1");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AiClassificationService(client, new ObjectMapper());
    }

    @Test
    void validClassification_isParsedAndReturned() {
        when(client.requestClassification(request)).thenReturn("""
                {
                  "intent": "INVOICE_SUBMISSION",
                  "confidence": 0.96,
                  "reason": "Sender is submitting an invoice.",
                  "evidence": ["Invoice attached for payment."],
                  "extractedData": {"invoiceNumber":"INV-1","amount":null,"vendor":null,"dueDate":null},
                  "intentProbabilities": {"INVOICE_SUBMISSION":0.96,"PAYMENT_QUERY":0.02,"DISPUTE":0.01,"SPAM":0.01}
                }
                """);

        ClassificationResult result = service.classify(request);

        assertEquals("INVOICE_SUBMISSION", result.intent());
        assertEquals(0.96, result.confidence());
        assertEquals("INV-1", result.extractedData().invoiceNumber());
        assertEquals(4, result.intentProbabilities().size());
    }

    @Test
    void unrecognizedIntent_throws() {
        when(client.requestClassification(request)).thenReturn("""
                {"intent": "NOT_A_REAL_INTENT", "confidence": 0.9, "reason": "x", "evidence": []}
                """);

        assertThrows(AIClassificationException.class, () -> service.classify(request));
    }

    @Test
    void confidenceOutOfRange_throws() {
        when(client.requestClassification(request)).thenReturn("""
                {"intent": "SPAM", "confidence": 1.5, "reason": "x", "evidence": []}
                """);

        assertThrows(AIClassificationException.class, () -> service.classify(request));
    }

    @Test
    void missingConfidence_throws() {
        when(client.requestClassification(request)).thenReturn("""
                {"intent": "SPAM", "reason": "x", "evidence": []}
                """);

        assertThrows(AIClassificationException.class, () -> service.classify(request));
    }

    @Test
    void malformedJson_throwsAIClassificationException() {
        when(client.requestClassification(request)).thenReturn("this is not json at all {{{");

        assertThrows(AIClassificationException.class, () -> service.classify(request));
    }

    @Test
    void missingIntentProbabilities_areFilledFromPrimaryIntent() {
        when(client.requestClassification(request)).thenReturn("""
                {"intent": "DISPUTE", "confidence": 0.8, "reason": "x", "evidence": []}
                """);

        ClassificationResult result = service.classify(request);

        assertEquals(4, result.intentProbabilities().size());
        assertEquals(0.8, result.intentProbabilities().get("DISPUTE"));
        assertEquals(0.0, result.intentProbabilities().get("SPAM"));
    }
}
