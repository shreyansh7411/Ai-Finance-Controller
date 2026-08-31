-- V3__Synthetic_ground_truth.sql

CREATE TABLE synthetic_ground_truth (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    scenario VARCHAR(50) NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    payment_id VARCHAR(100) NOT NULL,
    settlement_id VARCHAR(100),
    expected_outcome VARCHAR(100) NOT NULL,
    expected_difference DECIMAL(19,4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_synthetic_ground_truth_batch_id
    ON synthetic_ground_truth(batch_id);

CREATE INDEX idx_synthetic_ground_truth_payment_id
    ON synthetic_ground_truth(payment_id);

CREATE INDEX idx_synthetic_ground_truth_scenario
    ON synthetic_ground_truth(scenario);
