package com.mailops.service;

import com.mailops.dto.ClassificationResult;
import com.mailops.dto.EmailClassificationRequest;
import com.mailops.dto.ExtractedEmailData;
import com.mailops.dto.ProcessResultDto;
import com.mailops.entity.*;
import com.mailops.exception.AIClassificationException;
import com.mailops.repository.ActionRepository;
import com.mailops.repository.ClassificationRepository;
import com.mailops.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailProcessingServiceTest {

    @Mock private EmailRepository emailRepository;
    @Mock private ClassificationRepository classificationRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private AiClassificationService aiClassificationService;
    @Mock private ConfidenceService confidenceService;
    @Mock private ActionService actionService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EmailProcessingService service;

    private Email email(EmailStatus status) {
        return Email.builder().id(1L).sender("a@b.com").subject("Invoice").body("Please process invoice INV-1")
                .status(status).build();
    }

    @BeforeEach
    void setUp() {
        when(classificationRepository.save(any(Classification.class))).thenAnswer(inv -> {
            Classification c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
    }

    @Test
    void highConfidenceEmail_executesActionAndMarksProcessed() {
        Email e = email(EmailStatus.RECEIVED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(auditService.hasEventType(1L, AuditEventType.EMAIL_RECEIVED)).thenReturn(false);

        ClassificationResult result = new ClassificationResult("INVOICE_SUBMISSION", 0.95, "reason",
                List.of("evidence"), ExtractedEmailData.empty(), Map.of());
        when(aiClassificationService.classify(any(EmailClassificationRequest.class))).thenReturn(result);
        when(confidenceService.isAutonomous(0.95)).thenReturn(true);
        when(confidenceService.getThreshold()).thenReturn(0.85);
        when(actionService.actionTypeFor(Intent.INVOICE_SUBMISSION)).thenReturn(ActionType.CREATE_INVOICE);

        Action successAction = Action.builder().id(5L).emailId(1L).actionType(ActionType.CREATE_INVOICE)
                .actionStatus(ActionStatus.SUCCESS).result("Invoice created").build();
        when(actionService.execute(eq(1L), eq(Intent.INVOICE_SUBMISSION), any())).thenReturn(successAction);

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals("PROCESSED", dto.outcome());
        assertEquals("INVOICE_SUBMISSION", dto.intent());
        assertEquals(EmailStatus.PROCESSED, e.getStatus());
        verify(actionService).execute(eq(1L), eq(Intent.INVOICE_SUBMISSION), any());
        verify(auditService).log(eq(1L), eq(AuditEventType.ACTION_EXECUTED), eq(AuditActor.SYSTEM), anyString());
    }

    @Test
    void lowConfidenceEmail_becomesNeedsReview_andNoActionExecuted() {
        Email e = email(EmailStatus.RECEIVED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(auditService.hasEventType(1L, AuditEventType.EMAIL_RECEIVED)).thenReturn(false);

        ClassificationResult result = new ClassificationResult("PAYMENT_QUERY", 0.5, "reason",
                List.of(), ExtractedEmailData.empty(), Map.of());
        when(aiClassificationService.classify(any())).thenReturn(result);
        when(confidenceService.isAutonomous(0.5)).thenReturn(false);
        when(confidenceService.getThreshold()).thenReturn(0.85);

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals("NEEDS_REVIEW", dto.outcome());
        assertEquals(EmailStatus.NEEDS_REVIEW, e.getStatus());
        verify(actionService, never()).execute(anyLong(), any(), any());
        verify(auditService).log(eq(1L), eq(AuditEventType.HUMAN_REVIEW_REQUIRED), eq(AuditActor.SYSTEM), anyString());
    }

    @Test
    void aiFailure_resultsInNeedsReview() {
        Email e = email(EmailStatus.RECEIVED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(auditService.hasEventType(1L, AuditEventType.EMAIL_RECEIVED)).thenReturn(false);
        when(aiClassificationService.classify(any())).thenThrow(new AIClassificationException("timeout"));

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals("NEEDS_REVIEW", dto.outcome());
        assertEquals(EmailStatus.NEEDS_REVIEW, e.getStatus());
        verify(actionService, never()).execute(anyLong(), any(), any());
        verify(auditService).log(eq(1L), eq(AuditEventType.HUMAN_REVIEW_REQUIRED), eq(AuditActor.SYSTEM), anyString());
    }

    @Test
    void spamEmail_updatesStatusToSpam() {
        Email e = email(EmailStatus.RECEIVED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(auditService.hasEventType(1L, AuditEventType.EMAIL_RECEIVED)).thenReturn(false);

        ClassificationResult result = new ClassificationResult("SPAM", 0.99, "reason",
                List.of(), ExtractedEmailData.empty(), Map.of());
        when(aiClassificationService.classify(any())).thenReturn(result);
        when(confidenceService.isAutonomous(0.99)).thenReturn(true);
        when(confidenceService.getThreshold()).thenReturn(0.85);
        when(actionService.actionTypeFor(Intent.SPAM)).thenReturn(ActionType.FLAG_AS_SPAM);

        Action spamAction = Action.builder().id(6L).emailId(1L).actionType(ActionType.FLAG_AS_SPAM)
                .actionStatus(ActionStatus.SUCCESS).result("Email flagged as spam.").build();
        when(actionService.execute(eq(1L), eq(Intent.SPAM), any())).thenReturn(spamAction);

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals(EmailStatus.SPAM, e.getStatus());
        assertEquals("FLAG_AS_SPAM", dto.actionType());
    }

    @Test
    void failedAction_createsActionFailedAudit_andDoesNotReportSuccess() {
        Email e = email(EmailStatus.RECEIVED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(auditService.hasEventType(1L, AuditEventType.EMAIL_RECEIVED)).thenReturn(false);

        ClassificationResult result = new ClassificationResult("DISPUTE", 0.9, "reason",
                List.of(), ExtractedEmailData.empty(), Map.of());
        when(aiClassificationService.classify(any())).thenReturn(result);
        when(confidenceService.isAutonomous(0.9)).thenReturn(true);
        when(confidenceService.getThreshold()).thenReturn(0.85);
        when(actionService.actionTypeFor(Intent.DISPUTE)).thenReturn(ActionType.CREATE_FOLLOW_UP_TASK);

        Action failedAction = Action.builder().id(7L).emailId(1L).actionType(ActionType.CREATE_FOLLOW_UP_TASK)
                .actionStatus(ActionStatus.FAILED).result("Action failed: db error").build();
        when(actionService.execute(eq(1L), eq(Intent.DISPUTE), any())).thenReturn(failedAction);

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals("FAILED", dto.outcome());
        assertEquals(EmailStatus.FAILED, e.getStatus());
        verify(auditService).log(eq(1L), eq(AuditEventType.ACTION_FAILED), eq(AuditActor.SYSTEM), anyString());
    }

    @Test
    void alreadyProcessedEmail_isNotReprocessed() {
        Email e = email(EmailStatus.PROCESSED);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(actionRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        ProcessResultDto dto = service.processEmail(1L);

        assertEquals("ALREADY_PROCESSED", dto.outcome());
        verify(aiClassificationService, never()).classify(any());
        verify(actionService, never()).execute(anyLong(), any(), any());
    }
}
