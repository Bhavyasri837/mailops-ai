package com.mailops.controller;

import com.mailops.dto.*;
import com.mailops.entity.EmailStatus;
import com.mailops.service.EmailProcessingService;
import com.mailops.service.EmailQueryService;
import com.mailops.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin REST layer: parse/validate the request, delegate to a service, return
 * a DTO. No AI logic, no confidence logic, no action-selection logic, no
 * audit logic lives here - see EmailProcessingService/ReviewService.
 */
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailQueryService emailQueryService;
    private final EmailProcessingService emailProcessingService;
    private final ReviewService reviewService;

    @GetMapping
    public List<EmailDto> list(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) String search) {
        return emailQueryService.list(status, search);
    }

    @GetMapping("/{id}")
    public EmailDetailDto get(@PathVariable Long id) {
        return emailQueryService.getDetail(id);
    }

    @GetMapping("/{id}/classification")
    public ClassificationDto getClassification(@PathVariable Long id) {
        return emailQueryService.getLatestClassification(id);
    }

    @GetMapping("/{id}/actions")
    public List<ActionDto> getActions(@PathVariable Long id) {
        return emailQueryService.getActions(id);
    }

    @GetMapping("/{id}/audit")
    public List<AuditLogDto> getAudit(@PathVariable Long id) {
        return emailQueryService.getAuditLog(id);
    }

    @PostMapping("/{id}/process")
    public ProcessResultDto process(@PathVariable Long id) {
        return emailProcessingService.processEmail(id);
    }

    @PostMapping("/process-all")
    public ProcessAllResultDto processAll() {
        List<EmailDto> pending = emailQueryService.list(EmailStatus.RECEIVED, null);
        List<ProcessResultDto> results = new ArrayList<>();
        int processed = 0, needsReview = 0, failed = 0;

        for (EmailDto email : pending) {
            ProcessResultDto result = emailProcessingService.processEmail(email.id());
            results.add(result);
            switch (result.outcome()) {
                case "PROCESSED" -> processed++;
                case "NEEDS_REVIEW" -> needsReview++;
                case "FAILED" -> failed++;
                default -> { /* ALREADY_PROCESSED - not expected here, ignore */ }
            }
        }

        return new ProcessAllResultDto(pending.size(), processed, needsReview, failed, results);
    }

    @PostMapping("/{id}/review")
    public ProcessResultDto review(@PathVariable Long id, @Valid @RequestBody ReviewRequestDto request) {
        return reviewService.review(id, request.intent());
    }
}
