package com.mailops.service;

import com.mailops.dto.ExtractedEmailData;
import com.mailops.entity.Invoice;
import com.mailops.entity.InvoiceStatus;
import com.mailops.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Executes the CREATE_INVOICE action. Only ever writes fields the AI
 * actually extracted from the email text - never fabricates a value for a
 * field the model returned as null. Idempotent: the unique email_id
 * constraint on the invoices table is the final guard, but we check first
 * to avoid throwing in the common (single-caller) case.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoice(Long emailId, ExtractedEmailData data) {
        return invoiceRepository.findByEmailId(emailId).orElseGet(() -> {
            Invoice invoice = Invoice.builder()
                    .emailId(emailId)
                    .invoiceNumber(data != null ? data.invoiceNumber() : null)
                    .vendor(data != null ? data.vendor() : null)
                    .amount(parseAmount(data != null ? data.amount() : null))
                    // Not currently produced by the Phase 4 extraction schema
                    // (ExtractedEmailData has no currency field) - left null
                    // rather than guessed, per the "never invent data" rule.
                    .currency(null)
                    .dueDate(parseDate(data != null ? data.dueDate() : null))
                    .status(InvoiceStatus.LOGGED)
                    .build();
            try {
                return invoiceRepository.save(invoice);
            } catch (DataIntegrityViolationException e) {
                // Lost a race with a concurrent processing call - the other
                // call's invoice is the source of truth, use it.
                return invoiceRepository.findByEmailId(emailId).orElseThrow(() -> e);
            }
        });
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String cleaned = raw.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isBlank()) {
                return null;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse extracted invoice amount '{}' - leaving null.", raw);
            return null;
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            log.warn("Could not parse extracted due date '{}' - leaving null.", raw);
            return null;
        }
    }
}
