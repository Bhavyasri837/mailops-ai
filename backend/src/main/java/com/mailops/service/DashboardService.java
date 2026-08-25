package com.mailops.service;

import com.mailops.dto.DashboardStatsDto;
import com.mailops.entity.*;
import com.mailops.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmailRepository emailRepository;
    private final ClassificationRepository classificationRepository;
    private final ActionRepository actionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaskRepository taskRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardStatsDto getStats() {
        List<Email> allEmails = emailRepository.findAll();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EmailStatus s : EmailStatus.values()) {
            byStatus.put(s.name(), allEmails.stream().filter(e -> e.getStatus() == s).count());
        }

        List<Classification> allClassifications = classificationRepository.findAll();
        Map<String, Long> byIntent = new LinkedHashMap<>();
        for (Intent i : Intent.values()) {
            byIntent.put(i.name(), allClassifications.stream()
                    .filter(c -> c.getIntent() == i).count());
        }

        List<Action> allActions = actionRepository.findAll();
        long autonomousExecuted = auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getEventType() == AuditEventType.ACTION_EXECUTED && a.getActor() == AuditActor.SYSTEM)
                .count();
        long humanReviewed = auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getEventType() == AuditEventType.HUMAN_REVIEWED)
                .count();
        long actionsFailed = allActions.stream().filter(a -> a.getActionStatus() == ActionStatus.FAILED).count();

        return new DashboardStatsDto(
                allEmails.size(),
                byStatus,
                byIntent,
                autonomousExecuted,
                humanReviewed,
                byStatus.getOrDefault(EmailStatus.NEEDS_REVIEW.name(), 0L),
                invoiceRepository.count(),
                taskRepository.count(),
                replyDraftRepository.count(),
                byStatus.getOrDefault(EmailStatus.SPAM.name(), 0L),
                actionsFailed
        );
    }
}
