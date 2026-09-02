package com.aifincontroller.service;

import com.aifincontroller.config.DecisionProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionPolicyValidatorTest {

    private final DecisionPolicyValidator validator =
            new DecisionPolicyValidator();

    @Test
    void shouldAcceptValidThresholds() {

        DecisionProperties properties =
                new DecisionProperties();

        assertDoesNotThrow(
                () -> validator.validate(properties)
        );
    }

    @Test
    void shouldRejectNullProperties() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(null)
        );
    }

    @Test
    void shouldRejectMissingThreshold() {

        DecisionProperties properties =
                new DecisionProperties();

        properties.setHighConfidenceThreshold(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(properties)
        );
    }

    @Test
    void shouldRejectThresholdBelowZero() {

        DecisionProperties properties =
                new DecisionProperties();

        properties.setMediumConfidenceThreshold(
                new BigDecimal("-0.1")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(properties)
        );
    }

    @Test
    void shouldRejectThresholdAboveOne() {

        DecisionProperties properties =
                new DecisionProperties();

        properties.setHighConfidenceThreshold(
                new BigDecimal("1.1")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(properties)
        );
    }

    @Test
    void shouldRejectHighThresholdNotGreaterThanMedium() {

        DecisionProperties properties =
                new DecisionProperties();

        properties.setHighConfidenceThreshold(
                new BigDecimal("0.70")
        );

        properties.setMediumConfidenceThreshold(
                new BigDecimal("0.80")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(properties)
        );
    }
}
