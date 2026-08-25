package com.mailops.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfidenceServiceTest {

    private final ConfidenceService service = new ConfidenceService(0.85);

    @Test
    void confidenceAtOrAboveThreshold_isAutonomous() {
        assertTrue(service.isAutonomous(0.85));
        assertTrue(service.isAutonomous(0.99));
    }

    @Test
    void confidenceBelowThreshold_isNotAutonomous() {
        assertFalse(service.isAutonomous(0.84));
        assertFalse(service.isAutonomous(0.1));
    }
}
