package com.mailops.dto;

import com.mailops.entity.Task;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        Long emailId,
        String title,
        String description,
        String priority,
        String status,
        LocalDateTime createdAt
) {
    public static TaskDto from(Task t) {
        return new TaskDto(
                t.getId(), t.getEmailId(), t.getTitle(), t.getDescription(),
                t.getPriority().name(), t.getStatus().name(), t.getCreatedAt()
        );
    }
}
