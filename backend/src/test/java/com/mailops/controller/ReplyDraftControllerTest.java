package com.mailops.controller;

import com.mailops.entity.ReplyDraft;
import com.mailops.entity.ReplyDraftStatus;
import com.mailops.repository.ReplyDraftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReplyDraftController.class)
class ReplyDraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReplyDraftRepository replyDraftRepository;

    @Test
    void listReplyDrafts_returnsCleanDtoShape() throws Exception {
        ReplyDraft draft = ReplyDraft.builder()
                .id(1L)
                .emailId(5L)
                .draftText("Hi, your payment for invoice INV-9 was received on...")
                .status(ReplyDraftStatus.DRAFTED)
                .createdAt(LocalDateTime.now())
                .build();
        when(replyDraftRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draft));

        mockMvc.perform(get("/api/reply-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].emailId").value(5))
                .andExpect(jsonPath("$[0].draftText").exists())
                .andExpect(jsonPath("$[0].status").value("DRAFTED"))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void listReplyDrafts_empty_returnsEmptyArray() throws Exception {
        when(replyDraftRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/api/reply-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
