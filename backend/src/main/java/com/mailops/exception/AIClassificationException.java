package com.mailops.exception;

/**
 * Thrown for anything that prevents a trustworthy classification from being
 * produced: the provider being unreachable, a timeout, a malformed response,
 * or a response that fails validation. EmailProcessingService catches this
 * and routes the email to NEEDS_REVIEW rather than letting it propagate as
 * a 500 - an AI failure must never crash the request or the application.
 */
public class AIClassificationException extends RuntimeException {

    public AIClassificationException(String message) {
        super(message);
    }

    public AIClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
