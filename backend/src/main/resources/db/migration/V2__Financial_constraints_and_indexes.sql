-- V2__Financial_constraints_and_indexes.sql
-- Additive Phase 02A constraints/indexes. V1 was already applied locally.

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_amount_non_negative CHECK (amount >= 0);

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_amount_non_negative CHECK (amount >= 0);

ALTER TABLE settlements
    ADD CONSTRAINT ck_settlements_amount_non_negative CHECK (amount >= 0),
    ADD CONSTRAINT ck_settlements_fees_non_negative CHECK (fees >= 0),
    ADD CONSTRAINT ck_settlements_tax_non_negative CHECK (tax >= 0);

ALTER TABLE refunds
    ADD CONSTRAINT ck_refunds_amount_non_negative CHECK (amount >= 0);

ALTER TABLE reconciliation_results
    ADD CONSTRAINT ck_recon_results_confidence CHECK (
        confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)
    );

ALTER TABLE reconciliation_exceptions
    ADD CONSTRAINT ck_recon_exceptions_ai_confidence CHECK (
        ai_confidence IS NULL OR (ai_confidence >= 0 AND ai_confidence <= 1)
    );

-- UNIQUE (settlement_id, payment_id) does not collapse NULL payment_id values.
CREATE UNIQUE INDEX uk_settlements_settlement_id_null_payment
    ON settlements (settlement_id)
    WHERE payment_id IS NULL;

CREATE INDEX idx_settlements_utr ON settlements (utr);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
