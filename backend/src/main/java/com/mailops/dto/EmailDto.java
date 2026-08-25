package com.mailops.dto;

import com.mailops.entity.Email;
import com.mailops.entity.EmailStatus;

import java.time.LocalDateTime;

/**
 * Read-facing projection of an Email. Never expose the JPA entity directly
 * through the REST layer.
 */
public record EmailDto(
        Long id,
        String externalId,
        String sender,
        String recipient,
        String subject,
        String body,
        LocalDateTime receivedAt,
        EmailStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmailDto from(Email e) {
        return new EmailDto(
                e.getId(), e.getExternalId(), e.getSender(), e.getRecipient(),
                e.getSubject(), e.getBody(), e.getReceivedAt(), e.getStatus(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
