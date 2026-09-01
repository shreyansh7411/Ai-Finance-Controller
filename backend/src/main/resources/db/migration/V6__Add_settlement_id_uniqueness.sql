ALTER TABLE settlements
ADD CONSTRAINT uk_settlements_settlement_id UNIQUE (settlement_id);
