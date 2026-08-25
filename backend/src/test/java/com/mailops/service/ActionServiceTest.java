package com.mailops.service;

import com.mailops.dto.ExtractedEmailData;
import com.mailops.entity.*;
import com.mailops.repository.ActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock private ActionRepository actionRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private ReplyDraftService replyDraftService;
    @Mock private TaskService taskService;

    @InjectMocks
    private ActionService actionService;

    @Test
    void duplicateProcessing_doesNotCreateDuplicateAction() {
        Action existing = Action.builder().id(1L).emailId(1L).actionType(ActionType.CREATE_INVOICE)
                .actionStatus(ActionStatus.SUCCESS).result("Invoice created.").build();
        when(actionRepository.findByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(existing));

        Action result = actionService.execute(1L, Intent.INVOICE_SUBMISSION, ExtractedEmailData.empty());

        assertSame(existing, result);
        verify(invoiceService, never()).createInvoice(any(), any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    void invoiceSubmission_mapsToCreateInvoiceAction() {
        when(actionRepository.findByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        Invoice invoice = Invoice.builder().id(1L).emailId(1L).invoiceNumber("INV-1").build();
        when(invoiceService.createInvoice(eq(1L), any())).thenReturn(invoice);
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        Action result = actionService.execute(1L, Intent.INVOICE_SUBMISSION,
                new ExtractedEmailData("INV-1", "100.00", "Acme", null));

        assertEquals(ActionType.CREATE_INVOICE, result.getActionType());
        assertEquals(ActionStatus.SUCCESS, result.getActionStatus());
        verify(invoiceService).createInvoice(1L, new ExtractedEmailData("INV-1", "100.00", "Acme", null));
    }

    @Test
    void actionExecutionFailure_isRecordedAsFailedNotThrown() {
        when(actionRepository.findByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(taskService.createDisputeTask(1L)).thenThrow(new RuntimeException("db down"));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        Action result = actionService.execute(1L, Intent.DISPUTE, ExtractedEmailData.empty());

        assertEquals(ActionStatus.FAILED, result.getActionStatus());
        assertTrue(result.getResult().contains("db down"));
    }

    @Test
    void spamIntent_mapsToFlagAsSpam_withNoSideEffectServiceCalls() {
        when(actionRepository.findByEmailIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        Action result = actionService.execute(1L, Intent.SPAM, ExtractedEmailData.empty());

        assertEquals(ActionType.FLAG_AS_SPAM, result.getActionType());
        assertEquals(ActionStatus.SUCCESS, result.getActionStatus());
        verifyNoInteractions(invoiceService, replyDraftService, taskService);
    }
}
