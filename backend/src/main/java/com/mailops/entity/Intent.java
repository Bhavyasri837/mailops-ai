package com.mailops.entity;

/**
 * The four required business intents. SPAM is included as a real classification
 * outcome (not just a system state) per the assessment's category list.
 * NEEDS_REVIEW is deliberately NOT here - it is an Email/processing status,
 * never something the AI classifies an email "as".
 */
public enum Intent {
    INVOICE_SUBMISSION,
    PAYMENT_QUERY,
    DISPUTE,
    SPAM
}
