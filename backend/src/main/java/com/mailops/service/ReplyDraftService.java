package com.mailops.service;

import com.mailops.entity.ReplyDraft;
import com.mailops.entity.ReplyDraftStatus;
import com.mailops.repository.ReplyDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the CREATE_REPLY_DRAFT action for PAYMENT_QUERY emails. Only ever
 * stores a draft locally - never sends real email, per the Phase 5 spec.
 */
@Service
@RequiredArgsConstructor
public class ReplyDraftService {

    private static final String TEMPLATE = """
            Hello,

            Thank you for contacting us regarding your invoice.

            Our accounts team will review the payment status and provide an update.

            Regards,
            Accounts Payable""";

    private final ReplyDraftRepository replyDraftRepository;

    @Transactional
    public ReplyDraft createReplyDraft(Long emailId) {
        return replyDraftRepository.findByEmailId(emailId).orElseGet(() -> {
            ReplyDraft draft = ReplyDraft.builder()
                    .emailId(emailId)
                    .draftText(TEMPLATE)
                    .status(ReplyDraftStatus.DRAFTED)
                    .build();
            try {
                return replyDraftRepository.save(draft);
            } catch (DataIntegrityViolationException e) {
                return replyDraftRepository.findByEmailId(emailId).orElseThrow(() -> e);
            }
        });
    }
}
