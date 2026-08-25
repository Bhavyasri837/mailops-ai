package com.mailops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailops.dto.*;
import com.mailops.entity.EmailStatus;
import com.mailops.exception.EmailNotFoundException;
import com.mailops.exception.InvalidReviewException;
import com.mailops.service.EmailProcessingService;
import com.mailops.service.EmailQueryService;
import com.mailops.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for EmailController. The service layer is mocked -
 * these tests verify HTTP contract (status codes, JSON shape, routing to the
 * correct service methods), not business logic, which is already covered by
 * the Phase 5 service-layer tests. The AI layer is never invoked.
 */
@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailQueryService emailQueryService;

    @MockBean
    private EmailProcessingService emailProcessingService;

    @MockBean
    private ReviewService reviewService;

    private EmailDto sampleEmail(Long id, EmailStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new EmailDto(id, "ext-" + id, "vendor@example.com", "ap@company.com",
                "Invoice attached", "Please find attached invoice.", now, status, now, now);
    }

    // ---------- GET /api/emails ----------

    @Test
    void listEmails_returnsOk_withEmailList() throws Exception {
        when(emailQueryService.list(null, null)).thenReturn(List.of(sampleEmail(1L, EmailStatus.RECEIVED)));

        mockMvc.perform(get("/api/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }

    @Test
    void listEmails_filtersByStatus() throws Exception {
        when(emailQueryService.list(EmailStatus.NEEDS_REVIEW, null))
                .thenReturn(List.of(sampleEmail(2L, EmailStatus.NEEDS_REVIEW)));

        mockMvc.perform(get("/api/emails").param("status", "NEEDS_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEEDS_REVIEW"));

        verify(emailQueryService).list(EmailStatus.NEEDS_REVIEW, null);
    }

    @Test
    void listEmails_filtersBySearch() throws Exception {
        when(emailQueryService.list(null, "invoice"))
                .thenReturn(List.of(sampleEmail(3L, EmailStatus.RECEIVED)));

        mockMvc.perform(get("/api/emails").param("search", "invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(emailQueryService).list(null, "invoice");
    }

    // ---------- GET /api/emails/{id} ----------

    @Test
    void getEmail_returnsDetail() throws Exception {
        EmailDetailDto detail = new EmailDetailDto(sampleEmail(1L, EmailStatus.PROCESSED), null, List.of(), List.of());
        when(emailQueryService.getDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/emails/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email.id").value(1));
    }

    @Test
    void getEmail_notFound_returns404WithCleanBody() throws Exception {
        when(emailQueryService.getDetail(999L)).thenThrow(new EmailNotFoundException(999L));

        mockMvc.perform(get("/api/emails/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/emails/999"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- GET /api/emails/{id}/classification ----------

    @Test
    void getClassification_returnsClassificationInfo() throws Exception {
        ClassificationDto dto = new ClassificationDto(1L, 1L, "PAYMENT_QUERY", 0.92, "Asked about payment status",
                List.of("subject mentions invoice #123"), Map.of("invoiceNumber", "123"),
                Map.of("PAYMENT_QUERY", 0.92, "DISPUTE", 0.08), LocalDateTime.now());
        when(emailQueryService.getLatestClassification(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/emails/1/classification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("PAYMENT_QUERY"))
                .andExpect(jsonPath("$.confidence").value(0.92))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.extractedData.invoiceNumber").value("123"));
    }

    @Test
    void getClassification_emailNotFound_returns404() throws Exception {
        when(emailQueryService.getLatestClassification(999L)).thenThrow(new EmailNotFoundException(999L));

        mockMvc.perform(get("/api/emails/999/classification"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_FOUND"));
    }

    // ---------- GET /api/emails/{id}/actions ----------

    @Test
    void getActions_returnsActionList() throws Exception {
        ActionDto action = new ActionDto(1L, 1L, "CREATE_INVOICE", "SUCCESS", "Invoice INV-1 created", LocalDateTime.now());
        when(emailQueryService.getActions(1L)).thenReturn(List.of(action));

        mockMvc.perform(get("/api/emails/1/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("CREATE_INVOICE"))
                .andExpect(jsonPath("$[0].actionStatus").value("SUCCESS"));
    }

    // ---------- GET /api/emails/{id}/audit ----------

    @Test
    void getAudit_returnsTimelineOrderedRecords() throws Exception {
        AuditLogDto entry1 = new AuditLogDto(1L, 1L, "EMAIL_RECEIVED", "SYSTEM", "Email received.", Map.of(), LocalDateTime.now());
        AuditLogDto entry2 = new AuditLogDto(2L, 1L, "CLASSIFIED", "AI", "Classified as PAYMENT_QUERY.", Map.of(), LocalDateTime.now());
        when(emailQueryService.getAuditLog(1L)).thenReturn(List.of(entry1, entry2));

        mockMvc.perform(get("/api/emails/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].actor").value("SYSTEM"))
                .andExpect(jsonPath("$[1].actor").value("AI"));
    }

    @Test
    void getAudit_emailNotFound_returns404() throws Exception {
        when(emailQueryService.getAuditLog(999L)).thenThrow(new EmailNotFoundException(999L));

        mockMvc.perform(get("/api/emails/999/audit"))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /api/emails/{id}/process ----------

    @Test
    void processEmail_autonomous_returnsProcessedResult() throws Exception {
        ProcessResultDto result = ProcessResultDto.success(12L, "INVOICE_SUBMISSION", 0.96,
                "CREATE_INVOICE", "SUCCESS", "Invoice INV-12 created.");
        when(emailProcessingService.processEmail(12L)).thenReturn(result);

        mockMvc.perform(post("/api/emails/12/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(12))
                .andExpect(jsonPath("$.outcome").value("PROCESSED"))
                .andExpect(jsonPath("$.intent").value("INVOICE_SUBMISSION"))
                .andExpect(jsonPath("$.actionType").value("CREATE_INVOICE"))
                .andExpect(jsonPath("$.actionStatus").value("SUCCESS"));
    }

    @Test
    void processEmail_lowConfidence_returnsNeedsReview() throws Exception {
        ProcessResultDto result = ProcessResultDto.needsReview(15L, "PAYMENT_QUERY", 0.61,
                "Confidence below threshold; awaiting human review.");
        when(emailProcessingService.processEmail(15L)).thenReturn(result);

        mockMvc.perform(post("/api/emails/15/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.actionType").doesNotExist());
    }

    @Test
    void processEmail_notFound_returns404() throws Exception {
        when(emailProcessingService.processEmail(999L)).thenThrow(new EmailNotFoundException(999L));

        mockMvc.perform(post("/api/emails/999/process"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_FOUND"));
    }

    // ---------- POST /api/emails/process-all ----------

    @Test
    void processAll_aggregatesResults_andIsolatesFailures() throws Exception {
        EmailDto e1 = sampleEmail(1L, EmailStatus.RECEIVED);
        EmailDto e2 = sampleEmail(2L, EmailStatus.RECEIVED);
        EmailDto e3 = sampleEmail(3L, EmailStatus.RECEIVED);
        when(emailQueryService.list(EmailStatus.RECEIVED, null)).thenReturn(List.of(e1, e2, e3));

        when(emailProcessingService.processEmail(1L)).thenReturn(
                ProcessResultDto.success(1L, "INVOICE_SUBMISSION", 0.9, "CREATE_INVOICE", "SUCCESS", "ok"));
        when(emailProcessingService.processEmail(2L)).thenReturn(
                ProcessResultDto.needsReview(2L, "DISPUTE", 0.5, "low confidence"));
        when(emailProcessingService.processEmail(3L)).thenReturn(
                ProcessResultDto.failed(3L, "SPAM", 0.99, "FLAG_AS_SPAM", "downstream error"));

        mockMvc.perform(post("/api/emails/process-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempted").value(3))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.needsReview").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.results", hasSize(3)));

        // one email failing must not prevent the other two from being attempted
        verify(emailProcessingService).processEmail(1L);
        verify(emailProcessingService).processEmail(2L);
        verify(emailProcessingService).processEmail(3L);
    }

    @Test
    void processAll_noEligibleEmails_returnsZeroedSummary() throws Exception {
        when(emailQueryService.list(EmailStatus.RECEIVED, null)).thenReturn(List.of());

        mockMvc.perform(post("/api/emails/process-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempted").value(0))
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    // ---------- POST /api/emails/{id}/review ----------

    @Test
    void reviewEmail_validIntent_returnsActionResult() throws Exception {
        ProcessResultDto result = ProcessResultDto.success(15L, "DISPUTE", 0.61,
                "CREATE_FOLLOW_UP_TASK", "SUCCESS", "Follow-up task created.");
        when(reviewService.review(15L, "DISPUTE")).thenReturn(result);

        String body = objectMapper.writeValueAsString(Map.of("intent", "DISPUTE"));

        mockMvc.perform(post("/api/emails/15/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("DISPUTE"))
                .andExpect(jsonPath("$.actionType").value("CREATE_FOLLOW_UP_TASK"))
                .andExpect(jsonPath("$.actionStatus").value("SUCCESS"));
    }

    @Test
    void reviewEmail_blankIntent_returns400ValidationError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("intent", ""));

        mockMvc.perform(post("/api/emails/15/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(reviewService);
    }

    @Test
    void reviewEmail_missingIntentField_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/emails/15/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void reviewEmail_invalidIntentValue_returns400() throws Exception {
        when(reviewService.review(15L, "NOT_A_REAL_INTENT"))
                .thenThrow(new InvalidReviewException("'NOT_A_REAL_INTENT' is not a valid intent."));

        String body = objectMapper.writeValueAsString(Map.of("intent", "NOT_A_REAL_INTENT"));

        mockMvc.perform(post("/api/emails/15/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REVIEW"));
    }

    @Test
    void reviewEmail_emailNotAwaitingReview_returns400() throws Exception {
        when(reviewService.review(1L, "SPAM"))
                .thenThrow(new InvalidReviewException("Email 1 is not awaiting human review (current status: PROCESSED)."));

        String body = objectMapper.writeValueAsString(Map.of("intent", "SPAM"));

        mockMvc.perform(post("/api/emails/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REVIEW"));
    }

    @Test
    void reviewEmail_emailNotFound_returns404() throws Exception {
        when(reviewService.review(999L, "SPAM")).thenThrow(new EmailNotFoundException(999L));

        String body = objectMapper.writeValueAsString(Map.of("intent", "SPAM"));

        mockMvc.perform(post("/api/emails/999/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ---------- unexpected errors ----------

    @Test
    void unexpectedException_returns500_withNoStackTraceLeak() throws Exception {
        when(emailQueryService.getDetail(1L)).thenThrow(new RuntimeException("db connection lost"));

        mockMvc.perform(get("/api/emails/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again."))
                .andExpect(jsonPath("$.message", not(containsString("db connection lost"))));
    }
}
