package com.mailops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Fields the model is permitted to extract from an email. Every field is
 * nullable by design: the system prompt explicitly instructs the model to
 * return null rather than invent a value it can't find in the email text.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExtractedEmailData(
        String invoiceNumber,
        String amount,
        String vendor,
        String dueDate
) {
    public static ExtractedEmailData empty() {
        return new ExtractedEmailData(null, null, null, null);
    }
}
