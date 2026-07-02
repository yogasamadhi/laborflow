ALTER TABLE team_settlement
  ADD COLUMN auditor_id BIGINT,
  ADD COLUMN auditor_name VARCHAR(64),
  ADD COLUMN audited_at TIMESTAMP WITHOUT TIME ZONE,
  ADD COLUMN rejected_reason VARCHAR(255),
  ADD COLUMN paid_by BIGINT,
  ADD COLUMN paid_operator_name VARCHAR(64),
  ADD COLUMN payment_remark VARCHAR(255);

ALTER TABLE team_settlement
  DROP CONSTRAINT IF EXISTS ck_team_settlement_status;

ALTER TABLE team_settlement
  ADD CONSTRAINT ck_team_settlement_status CHECK (settlement_status IN ('draft', 'confirmed', 'rejected')) NOT VALID;

CREATE INDEX idx_team_settlement_status ON team_settlement(settlement_status, payment_status);
