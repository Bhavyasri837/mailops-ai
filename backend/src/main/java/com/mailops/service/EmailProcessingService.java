package com.mailops.service;

import com.mailops.dto.ClassificationResult;
import com.mailops.dto.EmailClassificationRequest;
import com.mailops.dto.ExtractedEmailData;
import com.mailops.dto.ProcessResultDto;
import com.mailops.entity.*;
import com.mailops.exception.AIClassificationException;
import com.mailops.exception.EmailNotFoundException;
import com.mailops.repository.ActionRepository;
import com.mailops.repository.ClassificationRepository;
import com.mailops.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The central orchestrator: the ONLY place that ties AI classification to
 * deterministic business action. Controllers never contain this logic.
 *
 * Flow: load -> (short-circuit if already processed) -> classify (AI) ->
 * validate/save -> check confidence -> autonomous action OR human review ->
 * audit trail throughout. The LLM is consulted exactly once here for
 * "what is this email"; every decision after that (threshold, which action,
 * whether it succeeded) is deterministic Java code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProcessingService {

    private final EmailRepository emailRepository;
    private final ClassificationRepository classificationRepository;
    private final ActionRepository actionRepository;
    private final AiClassificationService aiClassificationService;
    private final ConfidenceService confidenceService;
    private final ActionService actionService;
    private final AuditService auditService;

    @Transactional
    public ProcessResultDto processEmail(Long emailId) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new EmailNotFoundException(emailId));

        // Idempotency guard #1 (orchestration level): an email that has
        // already reached a terminal outcome is never reprocessed. The
        // action-level guards in ActionService/the per-type services plus
        // the DB unique constraints are the second and third layers.
        if (email.getStatus() == EmailStatus.PROCESSED || email.getStatus() == EmailStatus.SPAM) {
            return alreadyProcessedResult(email);
        }

        if (!auditService.hasEventType(emailId, AuditEventType.EMAIL_RECEIVED)) {
            auditService.log(emailId, AuditEventType.EMAIL_RECEIVED, AuditActor.SYSTEM,
                    "Email received from " + safe(email.getSender()) + " and queued for processing.");
        }

        email.setStatus(EmailStatus.PROCESSING);
        emailRepository.save(email);

        ClassificationResult classification;
        try {
            classification = aiClassificationService.classify(
                    new EmailClassificationRequest(email.getSender(), email.getSubject(), email.getBody()));
        } catch (AIClassificationException e) {
            email.setStatus(EmailStatus.NEEDS_REVIEW);
            emailRepository.save(email);
            auditService.log(emailId, AuditEventType.HUMAN_REVIEW_REQUIRED, AuditActor.SYSTEM,
                    "AI classification failed (" + e.getMessage() + "). Routed to human review.");
            return ProcessResultDto.needsReview(emailId, null, null,
                    "AI classification failed; email requires human review.");
        }

        Classification saved = classificationRepository.save(Classification.builder()
                .emailId(emailId)
                .intent(Intent.valueOf(classification.intent()))
                .confidence(classification.confidence())
                .reason(classification.reason())
                .evidence(classification.evidence())
                .extractedData(toMap(classification.extractedData()))
                .intentProbabilities(toObjectMap(classification.intentProbabilities()))
                .build());

        auditService.log(emailId, AuditEventType.CLASSIFIED, AuditActor.AI,
                "Email classified as %s with confidence %.2f.".formatted(saved.getIntent(), saved.getConfidence()));

        boolean autonomous = confidenceService.isAutonomous(saved.getConfidence());
        double threshold = confidenceService.getThreshold();
        auditService.log(emailId, AuditEventType.CONFIDENCE_CHECKED, AuditActor.SYSTEM,
                autonomous
                        ? "Confidence %.2f meets autonomous processing threshold %.2f.".formatted(saved.getConfidence(), threshold)
                        : "Confidence %.2f is below autonomous processing threshold %.2f.".formatted(saved.getConfidence(), threshold));

        if (!autonomous) {
            email.setStatus(EmailStatus.NEEDS_REVIEW);
            emailRepository.save(email);
            auditService.log(emailId, AuditEventType.HUMAN_REVIEW_REQUIRED, AuditActor.SYSTEM,
                    "Low-confidence classification (" + saved.getIntent() + ") routed to human review.");
            return ProcessResultDto.needsReview(emailId, saved.getIntent().name(), saved.getConfidence(),
                    "Confidence below threshold; awaiting human review.");
        }

        ActionType actionType = actionService.actionTypeFor(saved.getIntent());
        auditService.log(emailId, AuditEventType.ACTION_SELECTED, AuditActor.SYSTEM,
                "Action %s selected for intent %s.".formatted(actionType, saved.getIntent()));

        Action action = actionService.execute(emailId, saved.getIntent(), classification.extractedData());

        if (action.getActionStatus() == ActionStatus.FAILED) {
            email.setStatus(EmailStatus.FAILED);
            emailRepository.save(email);
            auditService.log(emailId, AuditEventType.ACTION_FAILED, AuditActor.SYSTEM, action.getResult());
            return ProcessResultDto.failed(emailId, saved.getIntent().name(), saved.getConfidence(),
                    action.getActionType().name(), action.getResult());
        }

        email.setStatus(saved.getIntent() == Intent.SPAM ? EmailStatus.SPAM : EmailStatus.PROCESSED);
        emailRepository.save(email);
        auditService.log(emailId, AuditEventType.ACTION_EXECUTED, AuditActor.SYSTEM, action.getResult());

        return ProcessResultDto.success(emailId, saved.getIntent().name(), saved.getConfidence(),
                action.getActionType().name(), action.getActionStatus().name(), action.getResult());
    }

    private ProcessResultDto alreadyProcessedResult(Email email) {
        Optional<Classification> classification =
                classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(email.getId());
        Optional<Action> action =
                actionRepository.findTopByEmailIdOrderByCreatedAtDesc(email.getId());
        return ProcessResultDto.alreadyProcessed(
                email.getId(),
                classification.map(c -> c.getIntent().name()).orElse(null),
                classification.map(Classification::getConfidence).orElse(null),
                action.map(a -> a.getActionType().name()).orElse(null),
                action.map(a -> a.getActionStatus().name()).orElse(null),
                "Email was already processed (status=" + email.getStatus() + "); no action re-executed.");
    }

    private Map<String, Object> toMap(ExtractedEmailData data) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (data == null) {
            return map;
        }
        map.put("invoiceNumber", data.invoiceNumber());
        map.put("amount", data.amount());
        map.put("vendor", data.vendor());
        map.put("dueDate", data.dueDate());
        return map;
    }

    private Map<String, Object> toObjectMap(Map<String, Double> probabilities) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (probabilities == null) {
            return map;
        }
        probabilities.forEach(map::put);
        return map;
    }

    private String safe(String s) {
        return s == null ? "unknown sender" : s;
    }
}
