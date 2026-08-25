package com.mailops.controller;

import com.mailops.dto.DashboardStatsDto;
import com.mailops.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void getStats_returnsDynamicCounts_notHardcoded() throws Exception {
        DashboardStatsDto stats = new DashboardStatsDto(
                20L,
                Map.of("RECEIVED", 2L, "PROCESSED", 12L, "NEEDS_REVIEW", 3L, "SPAM", 2L, "FAILED", 1L, "PROCESSING", 0L),
                Map.of("INVOICE_SUBMISSION", 8L, "PAYMENT_QUERY", 5L, "DISPUTE", 4L, "SPAM", 2L),
                12L, 3L, 3L, 8L, 4L, 5L, 2L, 1L
        );
        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmails").value(20))
                .andExpect(jsonPath("$.emailsByStatus.NEEDS_REVIEW").value(3))
                .andExpect(jsonPath("$.invoicesCreated").value(8))
                .andExpect(jsonPath("$.tasksCreated").value(4))
                .andExpect(jsonPath("$.replyDraftsCreated").value(5))
                .andExpect(jsonPath("$.spamFlagged").value(2))
                .andExpect(jsonPath("$.actionsFailed").value(1));
    }
}
