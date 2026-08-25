package com.mailops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/emails/{id}/review. "intent" must be one of the four
 * Intent enum values (case-insensitive) - validated in ReviewService, since
 * that validation is business logic, not a simple annotation constraint.
 */
public record ReviewRequestDto(
        @NotBlank(message = "intent is required") String intent
) {
}
