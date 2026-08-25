package com.mailops.dto;

import com.mailops.entity.Action;

import java.time.LocalDateTime;

public record ActionDto(
        Long id,
        Long emailId,
        String actionType,
        String actionStatus,
        String result,
        LocalDateTime createdAt
) {
    public static ActionDto from(Action a) {
        return new ActionDto(
                a.getId(), a.getEmailId(), a.getActionType().name(), a.getActionStatus().name(),
                a.getResult(), a.getCreatedAt()
        );
    }
}
