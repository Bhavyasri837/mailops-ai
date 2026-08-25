package com.mailops.service;

import com.mailops.dto.ProcessResultDto;
import com.mailops.entity.*;
import com.mailops.exception.InvalidReviewException;
import com.mailops.repository.ClassificationRepository;
import com.mailops.repository.EmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private EmailRepository emailRepository;
    @Mock private ClassificationRepository classificationRepository;
    @Mock private ActionService actionService;
    @Mock private AuditService auditService;

    @InjectMocks
    private ReviewService reviewService;

    private Email needsReviewEmail() {
        return Email.builder().id(1L).sender("a@b.com").subject("s").body("b")
                .status(EmailStatus.NEEDS_REVIEW).build();
    }

    private Action successAction(ActionType type, String result) {
        return Action.builder().id(9L).emailId(1L).actionType(type).actionStatus(ActionStatus.SUCCESS).result(result).build();
    }

    @Test
    void humanReview_invoiceSubmission_createsInvoice() {
        Email e = needsReviewEmail();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(actionService.actionTypeFor(Intent.INVOICE_SUBMISSION)).thenReturn(ActionType.CREATE_INVOICE);
        when(actionService.execute(eq(1L), eq(Intent.INVOICE_SUBMISSION), any()))
                .thenReturn(successAction(ActionType.CREATE_INVOICE, "Invoice record created."));

        ProcessResultDto dto = reviewService.review(1L, "INVOICE_SUBMISSION");

        assertEquals("PROCESSED", dto.outcome());
        assertEquals(EmailStatus.PROCESSED, e.getStatus());
        verify(auditService).log(eq(1L), eq(AuditEventType.HUMAN_REVIEWED), eq(AuditActor.HUMAN), anyString());
    }

    @Test
    void humanReview_paymentQuery_createsReplyDraft() {
        Email e = needsReviewEmail();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(actionService.actionTypeFor(Intent.PAYMENT_QUERY)).thenReturn(ActionType.CREATE_REPLY_DRAFT);
        when(actionService.execute(eq(1L), eq(Intent.PAYMENT_QUERY), any()))
                .thenReturn(successAction(ActionType.CREATE_REPLY_DRAFT, "Reply draft generated."));

        ProcessResultDto dto = reviewService.review(1L, "payment_query");

        assertEquals("CREATE_REPLY_DRAFT", dto.actionType());
        assertEquals(EmailStatus.PROCESSED, e.getStatus());
    }

    @Test
    void humanReview_dispute_createsTask() {
        Email e = needsReviewEmail();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(actionService.actionTypeFor(Intent.DISPUTE)).thenReturn(ActionType.CREATE_FOLLOW_UP_TASK);
        when(actionService.execute(eq(1L), eq(Intent.DISPUTE), any()))
                .thenReturn(successAction(ActionType.CREATE_FOLLOW_UP_TASK, "Follow-up task created."));

        ProcessResultDto dto = reviewService.review(1L, "DISPUTE");

        assertEquals("CREATE_FOLLOW_UP_TASK", dto.actionType());
        assertEquals(EmailStatus.PROCESSED, e.getStatus());
    }

    @Test
    void humanReview_spam_flagsSpam() {
        Email e = needsReviewEmail();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));
        when(classificationRepository.findTopByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(actionService.actionTypeFor(Intent.SPAM)).thenReturn(ActionType.FLAG_AS_SPAM);
        when(actionService.execute(eq(1L), eq(Intent.SPAM), any()))
                .thenReturn(successAction(ActionType.FLAG_AS_SPAM, "Email flagged as spam."));

        ProcessResultDto dto = reviewService.review(1L, "SPAM");

        assertEquals("FLAG_AS_SPAM", dto.actionType());
        assertEquals(EmailStatus.SPAM, e.getStatus());
    }

    @Test
    void invalidIntent_throwsInvalidReviewException() {
        Email e = needsReviewEmail();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(InvalidReviewException.class, () -> reviewService.review(1L, "NOT_REAL"));
        verify(actionService, never()).execute(anyLong(), any(), any());
    }

    @Test
    void emailNotAwaitingReview_throwsInvalidReviewException() {
        Email e = Email.builder().id(1L).status(EmailStatus.PROCESSED).build();
        when(emailRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(InvalidReviewException.class, () -> reviewService.review(1L, "SPAM"));
        verify(actionService, never()).execute(anyLong(), any(), any());
    }
}
