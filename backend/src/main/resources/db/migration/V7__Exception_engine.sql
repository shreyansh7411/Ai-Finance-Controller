-- Phase 04: Exception Engine

ALTER TABLE reconciliation_exceptions
    ADD COLUMN category VARCHAR(100);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN expected_amount DECIMAL(19,4);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN actual_amount DECIMAL(19,4);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN difference DECIMAL(19,4);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN source_reference VARCHAR(255);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN candidate_record VARCHAR(255);

ALTER TABLE reconciliation_exceptions
    ADD COLUMN evidence TEXT;

UPDATE reconciliation_exceptions
SET category = type
WHERE category IS NULL;

ALTER TABLE reconciliation_exceptions
    ALTER COLUMN category SET NOT NULL;

CREATE INDEX idx_recon_exceptions_category
    ON reconciliation_exceptions(category);

CREATE INDEX idx_recon_exceptions_severity
    ON reconciliation_exceptions(severity);

CREATE INDEX idx_recon_exceptions_category_status
    ON reconciliation_exceptions(category, status);
