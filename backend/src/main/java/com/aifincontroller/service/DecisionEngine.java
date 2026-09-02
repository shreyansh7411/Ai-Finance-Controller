package com.aifincontroller.service;

import com.aifincontroller.ai.dto.AiInvestigationResponse;
import com.aifincontroller.config.DecisionProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DecisionEngine {

    private final DecisionProperties properties;
    private final DecisionPolicyValidator policyValidator;

    public DecisionEngine(
            DecisionProperties properties,
            DecisionPolicyValidator policyValidator) {

        this.properties = properties;
        this.policyValidator = policyValidator;
    }

    public DecisionOutcome decide(
            AiInvestigationResponse response) {

        return decideWithReason(response).getOutcome();
    }

    public DecisionResult decideWithReason(
            AiInvestigationResponse response) {

        if (response == null) {
            throw new IllegalArgumentException(
                    "AI investigation response is required"
            );
        }

        policyValidator.validate(properties);

        DecisionResult result = new DecisionResult();

        result.setConfidence(response.getConfidence());

        BigDecimal confidence = response.getConfidence();

        if (confidence == null) {

            result.setOutcome(
                    DecisionOutcome.HUMAN_REVIEW
            );

            result.setReason(
                    "AI confidence is missing; exception must be escalated to human review."
            );

            return result;
        }

        String recommendedStatus =
                response.getRecommendedStatus();

        if ("INSUFFICIENT_EVIDENCE".equalsIgnoreCase(
                recommendedStatus)) {

            result.setOutcome(
                    DecisionOutcome.HUMAN_REVIEW
            );

            result.setReason(
                    "AI reported insufficient evidence; exception must be escalated to human review."
            );

            return result;
        }

        if (confidence.compareTo(
                properties.getHighConfidenceThreshold()) >= 0
                && "RESOLVED".equalsIgnoreCase(
                recommendedStatus)) {

            result.setOutcome(
                    DecisionOutcome.AUTO_RESOLVE
            );

            result.setReason(
                    "High-confidence AI investigation recommended resolution."
            );

            return result;
        }

        if (confidence.compareTo(
                properties.getMediumConfidenceThreshold()) >= 0) {

            result.setOutcome(
                    DecisionOutcome.REVIEW_RECOMMENDED
            );

            result.setReason(
                    "AI confidence is within the review range; human review is recommended."
            );

            return result;
        }

        result.setOutcome(
                DecisionOutcome.HUMAN_REVIEW
        );

        result.setReason(
                "AI confidence is below the review threshold; exception must be escalated to human review."
        );

        return result;
    }
}
