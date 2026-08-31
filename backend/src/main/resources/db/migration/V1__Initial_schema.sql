-- V1__Initial_schema.sql

-- Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_orders_order_id ON orders(order_id);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(100) UNIQUE NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    captured_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payments_payment_id ON payments(payment_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);

-- Settlements
CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(100) NOT NULL,
    payment_id VARCHAR(100),
    amount DECIMAL(19,4) NOT NULL,
    fees DECIMAL(19,4) NOT NULL,
    tax DECIMAL(19,4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    utr VARCHAR(100),
    settled_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (settlement_id, payment_id)
);
CREATE INDEX idx_settlements_payment_id ON settlements(payment_id);
CREATE INDEX idx_settlements_settlement_id ON settlements(settlement_id);

-- Refunds
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    refund_id VARCHAR(100) UNIQUE NOT NULL,
    payment_id VARCHAR(100) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);

-- Adjustments
CREATE TABLE adjustments (
    id BIGSERIAL PRIMARY KEY,
    adjustment_id VARCHAR(100) UNIQUE NOT NULL,
    settlement_id VARCHAR(100),
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_adjustments_settlement_id ON adjustments(settlement_id);

-- Reconciliation Results
CREATE TABLE reconciliation_results (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,
    matched_record VARCHAR(100),
    match_type VARCHAR(50) NOT NULL,
    expected_amount DECIMAL(19,4),
    actual_amount DECIMAL(19,4),
    difference DECIMAL(19,4),
    status VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(5,4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_recon_results_batch_id ON reconciliation_results(batch_id);
CREATE INDEX idx_recon_results_payment_ref ON reconciliation_results(payment_reference);

-- Exceptions
CREATE TABLE reconciliation_exceptions (
    id BIGSERIAL PRIMARY KEY,
    reconciliation_result_id BIGINT NOT NULL REFERENCES reconciliation_results(id),
    type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    evidence_summary TEXT,
    ai_confidence DECIMAL(5,4),
    resolution VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_recon_exceptions_result_id ON reconciliation_exceptions(reconciliation_result_id);
CREATE INDEX idx_recon_exceptions_status ON reconciliation_exceptions(status);

-- Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    evidence_reference TEXT,
    decision TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
