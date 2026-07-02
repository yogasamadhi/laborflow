ALTER TABLE salary_monthly_detail
  ADD COLUMN payslip_status VARCHAR(32) NOT NULL DEFAULT 'unconfirmed',
  ADD COLUMN payslip_confirmed_at TIMESTAMP WITHOUT TIME ZONE,
  ADD COLUMN payslip_feedback VARCHAR(500),
  ADD COLUMN payslip_feedback_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE salary_monthly_detail
  ADD CONSTRAINT ck_salary_monthly_detail_payslip_status CHECK (payslip_status IN ('unconfirmed', 'confirmed', 'exception')) NOT VALID;
