package com.mailops.service;

import com.mailops.dto.*;
import com.mailops.entity.Email;
import com.mailops.entity.EmailStatus;
import com.mailops.exception.EmailNotFoundException;
import com.mailops.repository.ActionRepository;
import com.mailops.repository.AuditLogRepository;
import com.mailops.repository.ClassificationRepository;
import com.mailops.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only projections for the email inbox views. Kept separate from
 * EmailProcessingService (which owns writes/business decisions) so the
 * controller layer for GET endpoints never touches processing logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailQueryService {

    private final EmailRepository emailRepository;
    private final ClassificationRepository classificationRepository;
    private final ActionRepository actionRepository;
    private final AuditLogRepository auditLogRepository;

    public List<EmailDto> list(EmailStatus status, String search) {
        return emailRepository.search(status, search).stream()
                .map(EmailDto::from)
                .toList();
    }

    public EmailDetailDto getDetail(Long emailId) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new EmailNotFoundException(emailId));

        ClassificationDto classificationDto = classificationRepository
                .findTopByEmailIdOrderByCreatedAtDesc(emailId)
                .map(ClassificationDto::from)
                .orElse(null);

        List<ActionDto> actions = actionRepository.findByEmailIdOrderByCreatedAtDesc(emailId).stream()
                .map(ActionDto::from)
                .toList();

        List<AuditLogDto> audit = auditLogRepository.findByEmailIdOrderByCreatedAtAsc(emailId).stream()
                .map(AuditLogDto::from)
                .toList();

        return new EmailDetailDto(EmailDto.from(email), classificationDto, actions, audit);
    }

    public ClassificationDto getLatestClassification(Long emailId) {
        assertEmailExists(emailId);
        return classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(emailId)
                .map(ClassificationDto::from)
                .orElse(null);
    }

    public List<ActionDto> getActions(Long emailId) {
        assertEmailExists(emailId);
        return actionRepository.findByEmailIdOrderByCreatedAtDesc(emailId).stream()
                .map(ActionDto::from)
                .toList();
    }

    public List<AuditLogDto> getAuditLog(Long emailId) {
        assertEmailExists(emailId);
        return auditLogRepository.findByEmailIdOrderByCreatedAtAsc(emailId).stream()
                .map(AuditLogDto::from)
                .toList();
    }

    private void assertEmailExists(Long emailId) {
        if (!emailRepository.existsById(emailId)) {
            throw new EmailNotFoundException(emailId);
        }
    }
}
