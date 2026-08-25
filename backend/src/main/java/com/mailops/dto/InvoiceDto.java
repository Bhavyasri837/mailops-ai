package com.mailops.dto;

import com.mailops.entity.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceDto(
        Long id,
        Long emailId,
        String invoiceNumber,
        String vendor,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        String status,
        LocalDateTime createdAt
) {
    public static InvoiceDto from(Invoice i) {
        return new InvoiceDto(
                i.getId(), i.getEmailId(), i.getInvoiceNumber(), i.getVendor(), i.getAmount(),
                i.getCurrency(), i.getDueDate(), i.getStatus().name(), i.getCreatedAt()
        );
    }
}
