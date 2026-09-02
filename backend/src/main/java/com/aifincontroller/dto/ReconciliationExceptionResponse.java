package com.aifincontroller.dto;

import java.math.BigDecimal;

public class ReconciliationExceptionResponse {

    private Long id;
    private String batchId;
    private String paymentReference;
    private String matchType;
    private String category;
    private String severity;
    private String status;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal difference;
    private BigDecimal confidenceScore;

    public ReconciliationExceptionResponse(
            Long id,
            String batchId,
            String paymentReference,
            String matchType,
            String category,
            String severity,
            String status,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            BigDecimal difference,
            BigDecimal confidenceScore) {

        this.id = id;
        this.batchId = batchId;
        this.paymentReference = paymentReference;
        this.matchType = matchType;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.difference = difference;
        this.confidenceScore = confidenceScore;
    }

    public Long getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getMatchType() {
        return matchType;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }
}
