package com.mailops.service;

import com.mailops.dto.ExtractedEmailData;
import com.mailops.entity.Invoice;
import com.mailops.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void invoiceSubmission_createsInvoiceWithExtractedData() {
        when(invoiceRepository.findByEmailId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = invoiceService.createInvoice(1L,
                new ExtractedEmailData("INV-42", "1,250.50", "Acme Corp", "2026-09-01"));

        assertEquals("INV-42", invoice.getInvoiceNumber());
        assertEquals("Acme Corp", invoice.getVendor());
        assertEquals(new BigDecimal("1250.50"), invoice.getAmount());
        assertNotNull(invoice.getDueDate());
    }

    @Test
    void missingExtractedFields_useNullNotFabricatedValues() {
        when(invoiceRepository.findByEmailId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = invoiceService.createInvoice(1L, ExtractedEmailData.empty());

        assertNull(invoice.getInvoiceNumber());
        assertNull(invoice.getVendor());
        assertNull(invoice.getAmount());
        assertNull(invoice.getDueDate());
    }

    @Test
    void duplicateInvoiceCall_returnsExistingInvoice_doesNotCreateSecond() {
        Invoice existing = Invoice.builder().id(1L).emailId(1L).invoiceNumber("INV-1").build();
        when(invoiceRepository.findByEmailId(1L)).thenReturn(Optional.of(existing));

        Invoice result = invoiceService.createInvoice(1L, new ExtractedEmailData("INV-1", null, null, null));

        assertSame(existing, result);
        verify(invoiceRepository, never()).save(any());
    }
}
