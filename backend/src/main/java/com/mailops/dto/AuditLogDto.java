package com.mailops.dto;

import com.mailops.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDto(
        Long id,
        Long emailId,
        String eventType,
        String actor,
        String message,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(), log.getEmailId(), log.getEventType().name(), log.getActor().name(),
                log.getMessage(), log.getMetadata(), log.getCreatedAt()
        );
    }
}
