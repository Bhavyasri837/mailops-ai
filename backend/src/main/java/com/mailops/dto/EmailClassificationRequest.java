package com.mailops.dto;

/**
 * The three untrusted fields passed to the classifier. Kept as a tiny,
 * explicit carrier (rather than passing the Email entity straight in) so
 * AiClassificationService/AiClassifierClient never touch persistence
 * concerns - they only ever see raw text to classify.
 */
public record EmailClassificationRequest(
        String sender,
        String subject,
        String body
) {
}
