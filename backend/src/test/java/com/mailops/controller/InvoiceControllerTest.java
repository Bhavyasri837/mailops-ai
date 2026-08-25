package com.mailops.controller;

import com.mailops.entity.Invoice;
import com.mailops.entity.InvoiceStatus;
import com.mailops.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceRepository invoiceRepository;

    @Test
    void listInvoices_returnsCleanDtoShape_noEntityLeak() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .emailId(10L)
                .invoiceNumber("INV-1001")
                .vendor("Acme Corp")
                .amount(new BigDecimal("1250.00"))
                .currency("USD")
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(invoiceRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(invoice));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-1001"))
                .andExpect(jsonPath("$[0].vendor").value("Acme Corp"))
                .andExpect(jsonPath("$[0].amount").value(1250.00))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].dueDate").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void listInvoices_empty_returnsEmptyArray() throws Exception {
        when(invoiceRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
