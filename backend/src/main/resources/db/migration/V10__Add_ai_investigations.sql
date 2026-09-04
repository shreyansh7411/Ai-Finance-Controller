CREATE TABLE ai_investigations (
    id BIGSERIAL PRIMARY KEY,
    exception_id BIGINT NOT NULL UNIQUE REFERENCES reconciliation_exceptions(id),
    conclusion TEXT NOT NULL,
    explanation TEXT NOT NULL,
    evidence_references TEXT,
    confidence DECIMAL(5,4),
    recommended_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_investigations_exception_id
    ON ai_investigations(exception_id);
