package com.mailops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MailOps AI - Autonomous Email-to-Action Agent
 *
 * Architecture: React -> Spring Boot -> OpenAI -> MySQL
 *
 * The LLM interprets email intent. Spring Boot validates that interpretation,
 * enforces the confidence threshold, and deterministically executes business
 * actions. MySQL stores every email, classification, action, and audit event.
 * Humans handle whatever the AI is not confident about.
 */
@SpringBootApplication
public class MailOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailOpsApplication.class, args);
    }
}
