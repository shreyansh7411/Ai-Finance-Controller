CREATE TABLE decision_records (
    id BIGSERIAL PRIMARY KEY,
    exception_id BIGINT NOT NULL UNIQUE,
    outcome VARCHAR(50) NOT NULL,
    confidence NUMERIC(5,4),
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_decision_records_exception_id
    ON decision_records(exception_id);
