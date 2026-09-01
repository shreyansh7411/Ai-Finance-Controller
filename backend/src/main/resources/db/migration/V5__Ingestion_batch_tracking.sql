-- V5__Ingestion_batch_tracking.sql

CREATE TABLE ingestion_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    filename VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    total_rows BIGINT NOT NULL DEFAULT 0,
    imported_rows BIGINT NOT NULL DEFAULT 0,
    skipped_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_ingestion_batches_batch_id
        UNIQUE (batch_id),

    CONSTRAINT ck_ingestion_batches_status
        CHECK (
            status IN (
                'PROCESSING',
                'COMPLETED',
                'COMPLETED_WITH_ERRORS',
                'FAILED'
            )
        ),

    CONSTRAINT ck_ingestion_batches_total_rows_non_negative
        CHECK (total_rows >= 0),

    CONSTRAINT ck_ingestion_batches_imported_rows_non_negative
        CHECK (imported_rows >= 0),

    CONSTRAINT ck_ingestion_batches_skipped_rows_non_negative
        CHECK (skipped_rows >= 0),

    CONSTRAINT ck_ingestion_batches_failed_rows_non_negative
        CHECK (failed_rows >= 0)
);

CREATE INDEX idx_ingestion_batches_status
    ON ingestion_batches(status);

CREATE INDEX idx_ingestion_batches_entity_type
    ON ingestion_batches(entity_type);

CREATE INDEX idx_ingestion_batches_started_at
    ON ingestion_batches(started_at);
