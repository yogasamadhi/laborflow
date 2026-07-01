CREATE TABLE salary_audit_record (
  id BIGINT PRIMARY KEY,
  salary_monthly_id BIGINT NOT NULL,
  salary_detail_id BIGINT,
  action VARCHAR(64) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  status_before VARCHAR(32),
  status_after VARCHAR(32),
  remark VARCHAR(255),
  proof_url VARCHAR(255),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salary_audit_monthly ON salary_audit_record(salary_monthly_id, created_at);
CREATE INDEX idx_salary_audit_detail ON salary_audit_record(salary_detail_id);
CREATE INDEX idx_salary_audit_action ON salary_audit_record(action);
