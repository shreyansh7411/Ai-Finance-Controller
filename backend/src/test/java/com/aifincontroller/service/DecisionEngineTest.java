package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.config.DecisionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionEngineTest {

    private DecisionEngine decisionEngine;

    @BeforeEach
    void setUp() {

        DecisionProperties properties =
                new DecisionProperties();

        properties.setHighConfidenceThreshold(
                new BigDecimal("0.90")
        );

        properties.setMediumConfidenceThreshold(
                new BigDecimal("0.70")
        );

        decisionEngine =
                new DecisionEngine(
                        properties,
                        new DecisionPolicyValidator()
                );
    }

    @Test
    void shouldAutoResolveHighConfidenceResolvedRecommendation() {

        AiInvestigationResponse response =
                response("0.95", "RESOLVED");

        assertEquals(
                DecisionOutcome.AUTO_RESOLVE,
                decisionEngine.decide(response)
        );
    }

    @Test
    void shouldAutoResolveAtHighConfidenceThreshold() {

        AiInvestigationResponse response =
                response("0.90", "RESOLVED");

        assertEquals(
                DecisionOutcome.AUTO_RESOLVE,
                decisionEngine.decide(response)
        );
    }

    @Test
    void shouldRecommendReviewForMediumConfidence() {

        AiInvestigationResponse response =
                response("0.80", "INVESTIGATING");

        DecisionResult result =
                decisionEngine.decideWithReason(response);

        assertEquals(
                DecisionOutcome.REVIEW_RECOMMENDED,
                result.getOutcome()
        );

        assertEquals(
                new BigDecimal("0.80"),
                result.getConfidence()
        );

        assertNotNull(result.getReason());
        assertFalse(result.getReason().isBlank());
    }

    @Test
    void shouldRecommendReviewAtMediumConfidenceThreshold() {

        AiInvestigationResponse response =
                response("0.70", "INVESTIGATING");

        DecisionResult result =
                decisionEngine.decideWithReason(response);

        assertEquals(
                DecisionOutcome.REVIEW_RECOMMENDED,
                result.getOutcome()
        );

        assertTrue(
                result.getReason().toLowerCase()
                        .contains("review")
        );
    }

    @Test
    void shouldRequireHumanReviewForLowConfidence() {

        AiInvestigationResponse response =
                response("0.50", "INVESTIGATING");

        DecisionResult result =
                decisionEngine.decideWithReason(response);

        assertEquals(
                DecisionOutcome.HUMAN_REVIEW,
                result.getOutcome()
        );

        assertNotNull(result.getReason());
        assertFalse(result.getReason().isBlank());
    }

    @Test
    void shouldRequireHumanReviewWhenEvidenceIsInsufficient() {

        AiInvestigationResponse response =
                response("0.99", "INSUFFICIENT_EVIDENCE");

        DecisionResult result =
                decisionEngine.decideWithReason(response);

        assertEquals(
                DecisionOutcome.HUMAN_REVIEW,
                result.getOutcome()
        );

        assertTrue(
                result.getReason().toLowerCase()
                        .contains("insufficient")
        );
    }

    @Test
    void shouldNotAutoResolveHighConfidenceInvestigatingRecommendation() {

        AiInvestigationResponse response =
                response("0.99", "INVESTIGATING");

        assertEquals(
                DecisionOutcome.REVIEW_RECOMMENDED,
                decisionEngine.decide(response)
        );
    }

    @Test
    void shouldRequireHumanReviewWhenConfidenceIsMissing() {

        AiInvestigationResponse response =
                response(null, "RESOLVED");

        DecisionResult result =
                decisionEngine.decideWithReason(response);

        assertEquals(
                DecisionOutcome.HUMAN_REVIEW,
                result.getOutcome()
        );

        assertTrue(
                result.getReason().toLowerCase()
                        .contains("confidence")
        );
    }

    @Test
    void shouldRejectNullResponse() {

        assertThrows(
                IllegalArgumentException.class,
                () -> decisionEngine.decide(null)
        );
    }

    private AiInvestigationResponse response(
            String confidence,
            String status) {

        AiInvestigationResponse response =
                new AiInvestigationResponse();

        response.setConclusion(
                "Supported conclusion"
        );

        response.setExplanation(
                "Supported explanation"
        );

        response.setEvidenceReferences(
                List.of("PAYMENT_AMOUNT")
        );

        response.setRecommendedStatus(status);

        if (confidence != null) {
            response.setConfidence(
                    new BigDecimal(confidence)
            );
        }

        return response;
    }
}
