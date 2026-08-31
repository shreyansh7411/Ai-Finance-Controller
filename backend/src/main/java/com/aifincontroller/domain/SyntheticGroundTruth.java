package com.aifincontroller.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "synthetic_ground_truth")
public class SyntheticGroundTruth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SyntheticScenario scenario;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    @Column(name = "settlement_id", length = 100)
    private String settlementId;

    @Column(name = "expected_outcome", nullable = false, length = 100)
    private String expectedOutcome;

    @Column(name = "expected_difference", precision = 19, scale = 4)
    private BigDecimal expectedDifference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public SyntheticScenario getScenario() {
        return scenario;
    }

    public void setScenario(SyntheticScenario scenario) {
        this.scenario = scenario;
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

    public String getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public BigDecimal getExpectedDifference() {
        return expectedDifference;
    }

    public void setExpectedDifference(BigDecimal expectedDifference) {
        this.expectedDifference = expectedDifference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
