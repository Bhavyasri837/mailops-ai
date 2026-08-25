package com.mailops.dto;

/**
 * Outcome of a single email processing attempt (autonomous or human-reviewed).
 * "outcome" is one of: PROCESSED, NEEDS_REVIEW, FAILED, ALREADY_PROCESSED.
 */
public record ProcessResultDto(
        Long emailId,
        String outcome,
        String intent,
        Double confidence,
        String actionType,
        String actionStatus,
        String message
) {
    public static ProcessResultDto success(Long emailId, String intent, Double confidence,
                                            String actionType, String actionStatus, String message) {
        return new ProcessResultDto(emailId, "PROCESSED", intent, confidence, actionType, actionStatus, message);
    }

    public static ProcessResultDto needsReview(Long emailId, String intent, Double confidence, String message) {
        return new ProcessResultDto(emailId, "NEEDS_REVIEW", intent, confidence, null, null, message);
    }

    public static ProcessResultDto failed(Long emailId, String intent, Double confidence,
                                           String actionType, String message) {
        return new ProcessResultDto(emailId, "FAILED", intent, confidence, actionType, "FAILED", message);
    }

    public static ProcessResultDto alreadyProcessed(Long emailId, String intent, Double confidence,
                                                      String actionType, String actionStatus, String message) {
        return new ProcessResultDto(emailId, "ALREADY_PROCESSED", intent, confidence, actionType, actionStatus, message);
    }
}
