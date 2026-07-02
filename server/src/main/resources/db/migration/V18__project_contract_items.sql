CREATE TABLE project_contract_item (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT,
  work_process_id BIGINT NOT NULL,
  item_code VARCHAR(64),
  item_name VARCHAR(120) NOT NULL,
  unit VARCHAR(32) NOT NULL,
  contract_quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  contract_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_project_contract_item_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  CONSTRAINT fk_project_contract_item_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  CONSTRAINT fk_project_contract_item_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  CONSTRAINT ck_project_contract_item_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  CONSTRAINT ck_project_contract_item_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  CONSTRAINT ck_project_contract_item_amounts CHECK (
    contract_quantity >= 0
    AND unit_price >= 0
    AND contract_amount >= 0
  ) NOT VALID
);

CREATE INDEX idx_project_contract_item_project ON project_contract_item(project_id, status);
CREATE INDEX idx_project_contract_item_scope ON project_contract_item(project_id, project_section_id, work_process_id);
CREATE UNIQUE INDEX uk_project_contract_item_code_active
  ON project_contract_item(project_id, item_code)
  WHERE deleted = 0 AND item_code IS NOT NULL;

ALTER TABLE daily_work_report
  ADD COLUMN contract_item_id BIGINT,
  ADD COLUMN output_value NUMERIC(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN pricing_basis VARCHAR(255);

ALTER TABLE daily_work_report
  ADD CONSTRAINT fk_daily_work_report_contract_item FOREIGN KEY (contract_item_id) REFERENCES project_contract_item(id) NOT VALID,
  ADD CONSTRAINT ck_daily_work_report_output_value CHECK (output_value >= 0) NOT VALID;

CREATE INDEX idx_daily_work_report_contract_item ON daily_work_report(contract_item_id);
