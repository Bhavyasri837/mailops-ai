package com.mailops.service;

import com.mailops.dto.ExtractedEmailData;
import com.mailops.dto.ProcessResultDto;
import com.mailops.entity.*;
import com.mailops.exception.EmailNotFoundException;
import com.mailops.exception.InvalidReviewException;
import com.mailops.repository.ClassificationRepository;
import com.mailops.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Handles human resolution of NEEDS_REVIEW emails. The human's selected
 * intent is authoritative - the LLM is never called again here. This is
 * deliberately a thin layer that reuses ActionService for the actual
 * business action, so INVOICE_SUBMISSION/PAYMENT_QUERY/DISPUTE/SPAM behave
 * identically whether they were decided autonomously or by a human.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final EmailRepository emailRepository;
    private final ClassificationRepository classificationRepository;
    private final ActionService actionService;
    private final AuditService auditService;

    @Transactional
    public ProcessResultDto review(Long emailId, String intentRaw) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new EmailNotFoundException(emailId));

        if (email.getStatus() != EmailStatus.NEEDS_REVIEW) {
            throw new InvalidReviewException(
                    "Email " + emailId + " is not awaiting human review (current status: " + email.getStatus() + ").");
        }

        Intent intent = parseIntent(intentRaw);

        auditService.log(emailId, AuditEventType.HUMAN_REVIEWED, AuditActor.HUMAN,
                "Human reviewer selected intent " + intent + ".");

        ActionType actionType = actionService.actionTypeFor(intent);
        auditService.log(emailId, AuditEventType.ACTION_SELECTED, AuditActor.SYSTEM,
                "Action " + actionType + " selected for human-reviewed intent " + intent + ".");

        Optional<Classification> latest =
                classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(emailId);
        ExtractedEmailData extractedData = latest
                .map(c -> mapToExtracted(c.getExtractedData()))
                .orElse(ExtractedEmailData.empty());
        Double confidence = latest.map(Classification::getConfidence).orElse(null);

        Action action = actionService.execute(emailId, intent, extractedData);

        if (action.getActionStatus() == ActionStatus.FAILED) {
            email.setStatus(EmailStatus.FAILED);
            emailRepository.save(email);
            auditService.log(emailId, AuditEventType.ACTION_FAILED, AuditActor.SYSTEM, action.getResult());
            return ProcessResultDto.failed(emailId, intent.name(), confidence,
                    action.getActionType().name(), action.getResult());
        }

        email.setStatus(intent == Intent.SPAM ? EmailStatus.SPAM : EmailStatus.PROCESSED);
        emailRepository.save(email);
        auditService.log(emailId, AuditEventType.ACTION_EXECUTED, AuditActor.SYSTEM, action.getResult());

        return ProcessResultDto.success(emailId, intent.name(), confidence,
                action.getActionType().name(), action.getActionStatus().name(), action.getResult());
    }

    private Intent parseIntent(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidReviewException("An intent selection is required.");
        }
        try {
            return Intent.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidReviewException("'" + raw + "' is not a valid intent. Valid values: "
                    + java.util.Arrays.toString(Intent.values()));
        }
    }

    private ExtractedEmailData mapToExtracted(Map<String, Object> data) {
        if (data == null) {
            return ExtractedEmailData.empty();
        }
        return new ExtractedEmailData(
                asString(data.get("invoiceNumber")),
                asString(data.get("amount")),
                asString(data.get("vendor")),
                asString(data.get("dueDate"))
        );
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
