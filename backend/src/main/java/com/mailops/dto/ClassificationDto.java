package com.mailops.dto;

import com.mailops.entity.Classification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ClassificationDto(
        Long id,
        Long emailId,
        String intent,
        Double confidence,
        String reason,
        List<String> evidence,
        Map<String, Object> extractedData,
        Map<String, Object> intentProbabilities,
        LocalDateTime createdAt
) {
    public static ClassificationDto from(Classification c) {
        return new ClassificationDto(
                c.getId(), c.getEmailId(), c.getIntent().name(), c.getConfidence(),
                c.getReason(), c.getEvidence(), c.getExtractedData(), c.getIntentProbabilities(),
                c.getCreatedAt()
        );
    }
}
