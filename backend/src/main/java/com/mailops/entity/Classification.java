package com.mailops.entity;

import com.mailops.util.MapJsonConverter;
import com.mailops.util.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One classification attempt for an email. Kept as a history table (many
 * Classification rows can reference the same Email) so re-classification
 * never destroys the prior AI decision - the API always surfaces the latest
 * one via ClassificationRepository.findTopByEmailIdOrderByCreatedAtDesc.
 */
@Entity
@Table(name = "classifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_id", nullable = false)
    private Long emailId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Intent intent;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 1000)
    private String reason;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> evidence;

    /**
     * Extracted fields (invoiceNumber, amount, vendor, dueDate, ...). Never
     * invented by the LLM - any field it could not find in the email is null.
     */
    @Convert(converter = MapJsonConverter.class)
    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private Map<String, Object> extractedData;

    /**
     * Probability-per-intent breakdown (e.g. {"PAYMENT_QUERY":0.61,"DISPUTE":0.27,...}).
     * This is genuine model output requested via the structured-output schema,
     * not fabricated after the fact - see AiClassificationService. Used to
     * render the "possible intents" breakdown on the Human Review page for
     * ambiguous/low-confidence emails.
     */
    @Convert(converter = MapJsonConverter.class)
    @Column(name = "intent_probabilities", columnDefinition = "TEXT")
    private Map<String, Object> intentProbabilities;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
