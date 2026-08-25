package com.mailops.dto;

import java.util.List;

/**
 * Full detail view for a single email: base fields plus its latest
 * classification (if any), all actions taken, and its full audit trail.
 */
public record EmailDetailDto(
        EmailDto email,
        ClassificationDto latestClassification,
        List<ActionDto> actions,
        List<AuditLogDto> auditLog
) {
}
