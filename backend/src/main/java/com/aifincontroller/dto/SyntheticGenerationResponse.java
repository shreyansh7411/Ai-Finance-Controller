package com.aifincontroller.dto;

import java.util.Map;

public class SyntheticGenerationResponse {

    private String batchId;
    private int requestedRecords;
    private int generatedOrders;
    private int generatedPayments;
    private int generatedSettlements;
    private int generatedRefunds;
    private int generatedAdjustments;
    private Map<String, Integer> scenarios;

    public SyntheticGenerationResponse(
            String batchId,
            int requestedRecords,
            int generatedOrders,
            int generatedPayments,
            int generatedSettlements,
            int generatedRefunds,
            int generatedAdjustments,
            Map<String, Integer> scenarios) {

        this.batchId = batchId;
        this.requestedRecords = requestedRecords;
        this.generatedOrders = generatedOrders;
        this.generatedPayments = generatedPayments;
        this.generatedSettlements = generatedSettlements;
        this.generatedRefunds = generatedRefunds;
        this.generatedAdjustments = generatedAdjustments;
        this.scenarios = scenarios;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getRequestedRecords() {
        return requestedRecords;
    }

    public int getGeneratedOrders() {
        return generatedOrders;
    }

    public int getGeneratedPayments() {
        return generatedPayments;
    }

    public int getGeneratedSettlements() {
        return generatedSettlements;
    }

    public int getGeneratedRefunds() {
        return generatedRefunds;
    }

    public int getGeneratedAdjustments() {
        return generatedAdjustments;
    }

    public Map<String, Integer> getScenarios() {
        return scenarios;
    }
}
