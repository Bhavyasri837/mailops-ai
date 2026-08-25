package com.mailops.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailops.dto.seed.SeedEmail;
import com.mailops.entity.Email;
import com.mailops.entity.EmailStatus;
import com.mailops.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Loads the 20 synthetic sample emails from sample-data/emails.json on
 * startup. Safe to run every time the application starts: each seed email
 * carries a stable externalId, and any externalId already present in the
 * database is skipped rather than re-inserted.
 *
 * Disabled entirely via SEED_ENABLED=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedService implements CommandLineRunner {

    private final EmailRepository emailRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("Seeding disabled (app.seed.enabled=false) - skipping sample data load.");
            return;
        }

        List<SeedEmail> seedEmails = loadSeedEmails();
        int inserted = 0;

        for (SeedEmail seed : seedEmails) {
            if (emailRepository.existsByExternalId(seed.externalId())) {
                continue;
            }
            Email email = Email.builder()
                    .externalId(seed.externalId())
                    .sender(seed.sender())
                    .recipient(seed.recipient())
                    .subject(seed.subject())
                    .body(seed.body())
                    .receivedAt(LocalDateTime.parse(seed.receivedAt()))
                    .status(EmailStatus.RECEIVED)
                    .build();
            emailRepository.save(email);
            inserted++;
        }

        log.info("Seed check complete: {} new sample email(s) inserted (of {} total in seed file).",
                inserted, seedEmails.size());
    }

    private List<SeedEmail> loadSeedEmails() throws Exception {
        try (InputStream is = new ClassPathResource("sample-data/emails.json").getInputStream()) {
            return objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SeedEmail.class));
        }
    }
}
