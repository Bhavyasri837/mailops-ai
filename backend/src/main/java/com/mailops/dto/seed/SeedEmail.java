package com.mailops.dto.seed;

/**
 * Deserialization target for sample-data/emails.json. Deliberately separate
 * from the Email entity so the seed file format can't accidentally leak JPA
 * concerns, and so this class lives entirely in a "seed-only" sub-package.
 */
public record SeedEmail(
        String externalId,
        String sender,
        String recipient,
        String subject,
        String body,
        String receivedAt
) {
}
