package com.mailops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Owns the single confidence-threshold decision: is this classification
 * trustworthy enough to act on autonomously, or must it go to a human?
 * Reads ai.confidence-threshold (CONFIDENCE_THRESHOLD env var, default 0.85
 * per application.properties) - the Phase 4 configuration is preserved
 * as-is, this class just centralizes the comparison so it lives in exactly
 * one place instead of being re-implemented per call site.
 */
@Service
public class ConfidenceService {

    private final double threshold;

    public ConfidenceService(@Value("${ai.confidence-threshold}") double threshold) {
        this.threshold = threshold;
    }

    public boolean isAutonomous(double confidence) {
        return confidence >= threshold;
    }

    public double getThreshold() {
        return threshold;
    }
}
