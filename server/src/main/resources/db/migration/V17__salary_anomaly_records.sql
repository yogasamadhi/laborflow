CREATE TABLE salary_anomaly_record (
  id BIGINT PRIMARY KEY,
  anomaly_key VARCHAR(160) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL DEFAULT 'open',
  handler_id BIGINT,
  handler_name VARCHAR(100),
  handled_at TIMESTAMP WITHOUT TIME ZONE,
  remark VARCHAR(500),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE salary_anomaly_record
  ADD CONSTRAINT ck_salary_anomaly_status CHECK (status IN ('open', 'handled', 'ignored', 'adjustment')) NOT VALID,
  ADD CONSTRAINT ck_salary_anomaly_deleted CHECK (deleted IN (0, 1)) NOT VALID;
