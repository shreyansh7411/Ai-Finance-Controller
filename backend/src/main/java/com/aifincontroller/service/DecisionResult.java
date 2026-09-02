package com.aifincontroller.service;

import java.math.BigDecimal;

public class DecisionResult {

    private DecisionOutcome outcome;
    private BigDecimal confidence;
    private String reason;

    public DecisionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(DecisionOutcome outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
