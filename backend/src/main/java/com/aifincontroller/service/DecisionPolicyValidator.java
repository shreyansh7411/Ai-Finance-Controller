package com.aifincontroller.service;

import com.aifincontroller.config.DecisionProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DecisionPolicyValidator {

    public void validate(DecisionProperties properties) {

        if (properties == null) {
            throw new IllegalArgumentException(
                    "Decision properties are required"
            );
        }

        BigDecimal high =
                properties.getHighConfidenceThreshold();

        BigDecimal medium =
                properties.getMediumConfidenceThreshold();

        if (high == null || medium == null) {
            throw new IllegalArgumentException(
                    "Decision confidence thresholds are required"
            );
        }

        if (medium.compareTo(BigDecimal.ZERO) < 0 ||
                medium.compareTo(BigDecimal.ONE) > 0) {

            throw new IllegalArgumentException(
                    "Medium confidence threshold must be between 0 and 1"
            );
        }

        if (high.compareTo(BigDecimal.ZERO) < 0 ||
                high.compareTo(BigDecimal.ONE) > 0) {

            throw new IllegalArgumentException(
                    "High confidence threshold must be between 0 and 1"
            );
        }

        if (high.compareTo(medium) <= 0) {
            throw new IllegalArgumentException(
                    "High confidence threshold must be greater than medium confidence threshold"
            );
        }
    }
}
