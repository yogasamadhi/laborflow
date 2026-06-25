CREATE TABLE project_payment_record (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  record_type VARCHAR(32) NOT NULL DEFAULT 'received',
  payment_date DATE NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  payer_name VARCHAR(120),
  voucher_file_id BIGINT,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_payment_project ON project_payment_record(project_id, payment_date);
CREATE INDEX idx_project_payment_type ON project_payment_record(project_id, record_type);

CREATE TABLE machinery_equipment (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  equipment_type VARCHAR(64) NOT NULL,
  owner_name VARCHAR(120),
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_machinery_equipment_type ON machinery_equipment(equipment_type);

CREATE TABLE machinery_work_record (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT,
  machinery_equipment_id BIGINT NOT NULL,
  work_date DATE NOT NULL,
  shift_count NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  idle_reason VARCHAR(255),
  operator_name VARCHAR(64),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_machinery_work_project_date ON machinery_work_record(project_id, work_date);

CREATE TABLE material_record (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT,
  material_name VARCHAR(120) NOT NULL,
  record_type VARCHAR(32) NOT NULL,
  record_date DATE NOT NULL,
  quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit VARCHAR(32),
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  supplier_name VARCHAR(120),
  impact_note VARCHAR(255),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_material_record_project_date ON material_record(project_id, record_date);
CREATE INDEX idx_material_record_type ON material_record(project_id, record_type);

CREATE TABLE project_document_archive (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT,
  work_process_id BIGINT,
  archive_date DATE NOT NULL,
  document_type VARCHAR(64) NOT NULL,
  file_id BIGINT NOT NULL,
  uploader_name VARCHAR(64),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_document_project_date ON project_document_archive(project_id, archive_date);
CREATE INDEX idx_project_document_type ON project_document_archive(project_id, document_type);
