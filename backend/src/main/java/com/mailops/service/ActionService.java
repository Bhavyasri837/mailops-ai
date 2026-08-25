package com.mailops.service;

import com.mailops.dto.ExtractedEmailData;
import com.mailops.entity.Action;
import com.mailops.entity.ActionStatus;
import com.mailops.entity.ActionType;
import com.mailops.entity.Intent;
import com.mailops.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The ONLY place in the codebase that maps a validated Intent to a
 * deterministic ActionType and actually executes it. The LLM never chooses
 * or invents an action - it only produces an Intent, which this fixed
 * mapping turns into one of exactly four predefined business operations.
 *
 * Idempotency: before doing any work, checks whether a SUCCESS action of the
 * target type already exists for this email and short-circuits if so. The
 * per-type services (InvoiceService/ReplyDraftService/TaskService) add a
 * second layer of protection via their own findByEmailId-or-create plus the
 * DB unique constraint, so duplicate records cannot be created even under
 * concurrent /process calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository actionRepository;
    private final InvoiceService invoiceService;
    private final ReplyDraftService replyDraftService;
    private final TaskService taskService;

    public ActionType actionTypeFor(Intent intent) {
        return switch (intent) {
            case INVOICE_SUBMISSION -> ActionType.CREATE_INVOICE;
            case PAYMENT_QUERY -> ActionType.CREATE_REPLY_DRAFT;
            case DISPUTE -> ActionType.CREATE_FOLLOW_UP_TASK;
            case SPAM -> ActionType.FLAG_AS_SPAM;
        };
    }

    @Transactional
    public Action execute(Long emailId, Intent intent, ExtractedEmailData extractedData) {
        ActionType type = actionTypeFor(intent);

        Optional<Action> existingSuccess = actionRepository.findByEmailIdOrderByCreatedAtDesc(emailId).stream()
                .filter(a -> a.getActionType() == type && a.getActionStatus() == ActionStatus.SUCCESS)
                .findFirst();
        if (existingSuccess.isPresent()) {
            log.info("Action {} already exists for email {} - skipping duplicate execution.", type, emailId);
            return existingSuccess.get();
        }

        String resultText;
        try {
            resultText = switch (intent) {
                case INVOICE_SUBMISSION -> {
                    var invoice = invoiceService.createInvoice(emailId, extractedData);
                    yield "Invoice record created" +
                            (invoice.getInvoiceNumber() != null ? " (" + invoice.getInvoiceNumber() + ")." : " (no invoice number extracted).");
                }
                case PAYMENT_QUERY -> {
                    replyDraftService.createReplyDraft(emailId);
                    yield "Reply draft generated for payment status inquiry.";
                }
                case DISPUTE -> {
                    taskService.createDisputeTask(emailId);
                    yield "Follow-up task created for invoice dispute.";
                }
                case SPAM -> "Email flagged as spam.";
            };
        } catch (Exception e) {
            log.error("Action {} failed for email {}", type, emailId, e);
            Action failed = Action.builder()
                    .emailId(emailId)
                    .actionType(type)
                    .actionStatus(ActionStatus.FAILED)
                    .result("Action failed: " + e.getMessage())
                    .build();
            return actionRepository.save(failed);
        }

        Action action = Action.builder()
                .emailId(emailId)
                .actionType(type)
                .actionStatus(ActionStatus.SUCCESS)
                .result(resultText)
                .build();
        return actionRepository.save(action);
    }
}
