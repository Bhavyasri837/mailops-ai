package com.mailops.dto;

import java.util.Map;

public record DashboardStatsDto(
        long totalEmails,
        Map<String, Long> emailsByStatus,
        Map<String, Long> classificationsByIntent,
        long autonomousActionsExecuted,
        long humanReviewsCompleted,
        long pendingHumanReview,
        long invoicesCreated,
        long tasksCreated,
        long replyDraftsCreated,
        long spamFlagged,
        long actionsFailed
) {
}
