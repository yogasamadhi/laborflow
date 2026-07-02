CREATE TABLE salary_deduction_record (
  id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  deduction_month VARCHAR(7) NOT NULL,
  deduction_type VARCHAR(32) NOT NULL,
  salary_item_id BIGINT,
  salary_item_name VARCHAR(100) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salary_deduction_employee_month ON salary_deduction_record(employee_id, deduction_month);
CREATE INDEX idx_salary_deduction_month_type ON salary_deduction_record(deduction_month, deduction_type);

ALTER TABLE salary_deduction_record
  ADD CONSTRAINT fk_salary_deduction_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT fk_salary_deduction_item FOREIGN KEY (salary_item_id) REFERENCES salary_item(id) NOT VALID,
  ADD CONSTRAINT ck_salary_deduction_month CHECK (deduction_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_salary_deduction_type CHECK (deduction_type IN ('social_security', 'housing_fund', 'individual_tax', 'advance', 'other')) NOT VALID,
  ADD CONSTRAINT ck_salary_deduction_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_salary_deduction_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_salary_deduction_amount CHECK (amount >= 0) NOT VALID;

INSERT INTO salary_item (id, name, type, is_fixed, sort_order, status)
VALUES
  (2301, '公积金扣款', 'deduction', 0, 125, 'active'),
  (2302, '个税扣款', 'deduction', 0, 126, 'active')
ON CONFLICT (id) DO NOTHING;
