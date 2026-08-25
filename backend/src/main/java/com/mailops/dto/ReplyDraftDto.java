package com.mailops.dto;

import com.mailops.entity.ReplyDraft;

import java.time.LocalDateTime;

public record ReplyDraftDto(
        Long id,
        Long emailId,
        String draftText,
        String status,
        LocalDateTime createdAt
) {
    public static ReplyDraftDto from(ReplyDraft r) {
        return new ReplyDraftDto(
                r.getId(), r.getEmailId(), r.getDraftText(), r.getStatus().name(), r.getCreatedAt()
        );
    }
}
