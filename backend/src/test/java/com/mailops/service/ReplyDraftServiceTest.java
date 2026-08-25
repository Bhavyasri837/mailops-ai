package com.mailops.service;

import com.mailops.entity.ReplyDraft;
import com.mailops.repository.ReplyDraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplyDraftServiceTest {

    @Mock private ReplyDraftRepository replyDraftRepository;

    @InjectMocks
    private ReplyDraftService replyDraftService;

    @Test
    void paymentQuery_createsReplyDraft() {
        when(replyDraftRepository.findByEmailId(1L)).thenReturn(Optional.empty());
        when(replyDraftRepository.save(any(ReplyDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        ReplyDraft draft = replyDraftService.createReplyDraft(1L);

        assertNotNull(draft.getDraftText());
        assertTrue(draft.getDraftText().contains("Accounts Payable"));
    }

    @Test
    void duplicateCall_returnsExistingDraft() {
        ReplyDraft existing = ReplyDraft.builder().id(1L).emailId(1L).draftText("x").build();
        when(replyDraftRepository.findByEmailId(1L)).thenReturn(Optional.of(existing));

        ReplyDraft result = replyDraftService.createReplyDraft(1L);

        assertSame(existing, result);
        verify(replyDraftRepository, never()).save(any());
    }
}
