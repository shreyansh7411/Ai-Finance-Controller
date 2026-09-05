package com.aifincontroller.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AiInvestigationRequest {

    private Long exceptionId;
    private String exceptionType;
    private String category;
    private String severity;

    private String paymentReference;
    private String orderId;
    private String paymentId;

    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal difference;

    private PaymentEvidence payment;
    private OrderEvidence order;
    private SettlementEvidence settlement;
    private FinancialAnalysis financialAnalysis;

    private List<RefundEvidence> refunds;
    private List<AdjustmentEvidence> adjustments;

    private String evidenceSummary;

    private List<String> evidenceIds;

    public FinancialAnalysis getFinancialAnalysis() {
        return financialAnalysis;
    }

    public void setFinancialAnalysis(FinancialAnalysis financialAnalysis) {
        this.financialAnalysis = financialAnalysis;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long exceptionId) {
        this.exceptionId = exceptionId;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public PaymentEvidence getPayment() {
        return payment;
    }

    public void setPayment(PaymentEvidence payment) {
        this.payment = payment;
    }

    public OrderEvidence getOrder() {
        return order;
    }

    public void setOrder(OrderEvidence order) {
        this.order = order;
    }

    public SettlementEvidence getSettlement() {
        return settlement;
    }

    public void setSettlement(SettlementEvidence settlement) {
        this.settlement = settlement;
    }

    public List<RefundEvidence> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<RefundEvidence> refunds) {
        this.refunds = refunds;
    }

    public List<AdjustmentEvidence> getAdjustments() {
        return adjustments;
    }

    public void setAdjustments(List<AdjustmentEvidence> adjustments) {
        this.adjustments = adjustments;
    }

    public String getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(String evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds;
    }

    public static class PaymentEvidence {
        private String paymentId;
        private String orderId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private Instant capturedAt;
        private Instant createdAt;

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getCapturedAt() {
            return capturedAt;
        }

        public void setCapturedAt(Instant capturedAt) {
            this.capturedAt = capturedAt;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class OrderEvidence {
        private String orderId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private Instant createdAt;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class SettlementEvidence {
        private String settlementId;
        private String paymentId;
        private BigDecimal amount;
        private BigDecimal fees;
        private BigDecimal tax;
        private String status;
        private String utr;
        private Instant settledAt;

        public String getSettlementId() {
            return settlementId;
        }

        public void setSettlementId(String settlementId) {
            this.settlementId = settlementId;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getFees() {
            return fees;
        }

        public void setFees(BigDecimal fees) {
            this.fees = fees;
        }

        public BigDecimal getTax() {
            return tax;
        }

        public void setTax(BigDecimal tax) {
            this.tax = tax;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getUtr() {
            return utr;
        }

        public void setUtr(String utr) {
            this.utr = utr;
        }

        public Instant getSettledAt() {
            return settledAt;
        }

        public void setSettledAt(Instant settledAt) {
            this.settledAt = settledAt;
        }
    }

    public static class RefundEvidence {
        private String refundId;
        private String paymentId;
        private BigDecimal amount;
        private String status;
        private Instant createdAt;

        public String getRefundId() {
            return refundId;
        }

        public void setRefundId(String refundId) {
            this.refundId = refundId;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class AdjustmentEvidence {
        private String adjustmentId;
        private String settlementId;
        private BigDecimal amount;
        private String type;
        private String description;
        private Instant createdAt;

        public String getAdjustmentId() {
            return adjustmentId;
        }

        public void setAdjustmentId(String adjustmentId) {
            this.adjustmentId = adjustmentId;
        }

        public String getSettlementId() {
            return settlementId;
        }

        public void setSettlementId(String settlementId) {
            this.settlementId = settlementId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
