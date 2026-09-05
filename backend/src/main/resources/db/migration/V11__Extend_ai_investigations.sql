ALTER TABLE ai_investigations
    ADD COLUMN what_happened TEXT,
    ADD COLUMN root_cause TEXT,
    ADD COLUMN financial_impact TEXT,
    ADD COLUMN supporting_evidence TEXT,
    ADD COLUMN alternative_explanations TEXT,
    ADD COLUMN missing_evidence TEXT,
    ADD COLUMN confidence_reasoning TEXT,
    ADD COLUMN recommended_action TEXT;
