package com.mailops.service;

import com.mailops.entity.AuditActor;
import com.mailops.entity.AuditEventType;
import com.mailops.entity.AuditLog;
import com.mailops.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The single write path for audit_logs. Every state transition in the
 * system (classification, confidence check, action selection/execution,
 * human review) must go through here so the audit trail is complete and
 * consistently shaped - never insert an AuditLog anywhere else.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog log(Long emailId, AuditEventType eventType, AuditActor actor, String message) {
        return log(emailId, eventType, actor, message, Map.of());
    }

    public AuditLog log(Long emailId, AuditEventType eventType, AuditActor actor, String message,
                         Map<String, Object> metadata) {
        AuditLog entry = AuditLog.builder()
                .emailId(emailId)
                .eventType(eventType)
                .actor(actor)
                .message(message)
                .metadata(metadata == null ? Map.of() : metadata)
                .build();
        return auditLogRepository.save(entry);
    }

    public List<AuditLog> getForEmail(Long emailId) {
        return auditLogRepository.findByEmailIdOrderByCreatedAtAsc(emailId);
    }

    public boolean hasEventType(Long emailId, AuditEventType type) {
        return getForEmail(emailId).stream().anyMatch(a -> a.getEventType() == type);
    }
}
