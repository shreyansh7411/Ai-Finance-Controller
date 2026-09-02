package com.aifincontroller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "decision")
public class DecisionProperties {

    private BigDecimal highConfidenceThreshold =
            new BigDecimal("0.90");

    private BigDecimal mediumConfidenceThreshold =
            new BigDecimal("0.70");

    public BigDecimal getHighConfidenceThreshold() {
        return highConfidenceThreshold;
    }

    public void setHighConfidenceThreshold(
            BigDecimal highConfidenceThreshold) {
        this.highConfidenceThreshold = highConfidenceThreshold;
    }

    public BigDecimal getMediumConfidenceThreshold() {
        return mediumConfidenceThreshold;
    }

    public void setMediumConfidenceThreshold(
            BigDecimal mediumConfidenceThreshold) {
        this.mediumConfidenceThreshold = mediumConfidenceThreshold;
    }
}
