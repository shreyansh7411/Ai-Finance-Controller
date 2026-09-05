package com.aifincontroller.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public class FinancialAnalysis {

    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal reconciliationDifference;

    private BigDecimal paymentAmount;
    private BigDecimal orderAmount;

    private BigDecimal settlementAmount;
    private BigDecimal settlementFees;
    private BigDecimal settlementTax;

    private BigDecimal totalRefundAmount;
    private BigDecimal totalAdjustmentAmount;

    private BigDecimal knownDeductions;
    private BigDecimal explainedDifference;
    private BigDecimal unexplainedDifference;

    private boolean paymentMatchesOrder;
    private boolean paymentMatchesExpected;
    private boolean settlementMatchesActual;
    private boolean differenceFullyExplained;

    private boolean settlementPresent;
    private boolean refundPresent;
    private boolean adjustmentPresent;

    private List<String> candidateCauses;
    private List<String> contradictions;
    private List<String> missingEvidence;

    private String financialAssessment;

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

    public BigDecimal getReconciliationDifference() {
        return reconciliationDifference;
    }

    public void setReconciliationDifference(BigDecimal reconciliationDifference) {
        this.reconciliationDifference = reconciliationDifference;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public BigDecimal getSettlementFees() {
        return settlementFees;
    }

    public void setSettlementFees(BigDecimal settlementFees) {
        this.settlementFees = settlementFees;
    }

    public BigDecimal getSettlementTax() {
        return settlementTax;
    }

    public void setSettlementTax(BigDecimal settlementTax) {
        this.settlementTax = settlementTax;
    }

    public BigDecimal getTotalRefundAmount() {
        return totalRefundAmount;
    }

    public void setTotalRefundAmount(BigDecimal totalRefundAmount) {
        this.totalRefundAmount = totalRefundAmount;
    }

    public BigDecimal getTotalAdjustmentAmount() {
        return totalAdjustmentAmount;
    }

    public void setTotalAdjustmentAmount(BigDecimal totalAdjustmentAmount) {
        this.totalAdjustmentAmount = totalAdjustmentAmount;
    }

    public BigDecimal getKnownDeductions() {
        return knownDeductions;
    }

    public void setKnownDeductions(BigDecimal knownDeductions) {
        this.knownDeductions = knownDeductions;
    }

    public BigDecimal getExplainedDifference() {
        return explainedDifference;
    }

    public void setExplainedDifference(BigDecimal explainedDifference) {
        this.explainedDifference = explainedDifference;
    }

    public BigDecimal getUnexplainedDifference() {
        return unexplainedDifference;
    }

    public void setUnexplainedDifference(BigDecimal unexplainedDifference) {
        this.unexplainedDifference = unexplainedDifference;
    }

    public boolean isPaymentMatchesOrder() {
        return paymentMatchesOrder;
    }

    public void setPaymentMatchesOrder(boolean paymentMatchesOrder) {
        this.paymentMatchesOrder = paymentMatchesOrder;
    }

    public boolean isPaymentMatchesExpected() {
        return paymentMatchesExpected;
    }

    public void setPaymentMatchesExpected(boolean paymentMatchesExpected) {
        this.paymentMatchesExpected = paymentMatchesExpected;
    }

    public boolean isSettlementMatchesActual() {
        return settlementMatchesActual;
    }

    public void setSettlementMatchesActual(boolean settlementMatchesActual) {
        this.settlementMatchesActual = settlementMatchesActual;
    }

    public boolean isDifferenceFullyExplained() {
        return differenceFullyExplained;
    }

    public void setDifferenceFullyExplained(boolean differenceFullyExplained) {
        this.differenceFullyExplained = differenceFullyExplained;
    }

    public boolean isSettlementPresent() {
        return settlementPresent;
    }

    public void setSettlementPresent(boolean settlementPresent) {
        this.settlementPresent = settlementPresent;
    }

    public boolean isRefundPresent() {
        return refundPresent;
    }

    public void setRefundPresent(boolean refundPresent) {
        this.refundPresent = refundPresent;
    }

    public boolean isAdjustmentPresent() {
        return adjustmentPresent;
    }

    public void setAdjustmentPresent(boolean adjustmentPresent) {
        this.adjustmentPresent = adjustmentPresent;
    }

    public List<String> getCandidateCauses() {
        return candidateCauses;
    }

    public void setCandidateCauses(List<String> candidateCauses) {
        this.candidateCauses = candidateCauses;
    }

    public List<String> getContradictions() {
        return contradictions;
    }

    public void setContradictions(List<String> contradictions) {
        this.contradictions = contradictions;
    }

    public List<String> getMissingEvidence() {
        return missingEvidence;
    }

    public void setMissingEvidence(List<String> missingEvidence) {
        this.missingEvidence = missingEvidence;
    }

    public String getFinancialAssessment() {
        return financialAssessment;
    }

    public void setFinancialAssessment(String financialAssessment) {
        this.financialAssessment = financialAssessment;
    }
}
