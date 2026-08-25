package com.mailops.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error mapping. No stack trace or internal exception detail is ever
 * returned to the frontend - only a stable error code, an HTTP status, a
 * user-safe message, and the request path.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EmailNotFoundException e, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, "EMAIL_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidReviewException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidReview(InvalidReviewException e, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_REVIEW", e.getMessage(), request);
    }

    @ExceptionHandler(AIClassificationException.class)
    public ResponseEntity<Map<String, Object>> handleAiFailure(AIClassificationException e, HttpServletRequest request) {
        // Never surfaced as a 500 - the caller already routed the email to
        // NEEDS_REVIEW; this only fires if the exception escapes that path.
        return body(HttpStatus.BAD_GATEWAY, "AI_CLASSIFICATION_FAILED",
                "The AI classification service failed. The email has been routed for human review.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request.");
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again.", request);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message,
                                                       HttpServletRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", LocalDateTime.now());
        payload.put("status", status.value());
        payload.put("error", code);
        payload.put("message", message);
        payload.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(payload);
    }
}
