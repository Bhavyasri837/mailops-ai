package com.mailops.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The unique constraint on email_id is the DB-level idempotency guard:
 * one email can never produce more than one Invoice row, even if
 * /process is called concurrently or repeatedly.
 */
@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(columnNames = "email_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_id", nullable = false, unique = true)
    private Long emailId;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(length = 255)
    private String vendor;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.LOGGED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
