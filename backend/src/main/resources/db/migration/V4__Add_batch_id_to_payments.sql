-- V4__Add_batch_id_to_payments.sql

ALTER TABLE payments
    ADD COLUMN batch_id VARCHAR(100);

CREATE INDEX idx_payments_batch_id
    ON payments(batch_id);
