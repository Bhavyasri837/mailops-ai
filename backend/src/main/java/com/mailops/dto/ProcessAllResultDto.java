package com.mailops.dto;

import java.util.List;

public record ProcessAllResultDto(
        int totalAttempted,
        int processed,
        int needsReview,
        int failed,
        List<ProcessResultDto> results
) {
}
