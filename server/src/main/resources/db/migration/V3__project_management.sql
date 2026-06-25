CREATE TABLE project (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  client_name VARCHAR(120),
  contract_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  contract_start_date DATE,
  contract_end_date DATE,
  project_manager VARCHAR(64),
  address VARCHAR(255),
  scope TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'not_started',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_name ON project(name);
CREATE INDEX idx_project_status ON project(status);

CREATE TABLE project_section (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  bid_section_name VARCHAR(120),
  name VARCHAR(120) NOT NULL,
  start_stake_no VARCHAR(64),
  end_stake_no VARCHAR(64),
  start_well_no VARCHAR(64),
  end_well_no VARCHAR(64),
  length NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_section_project_id ON project_section(project_id);

CREATE TABLE work_process (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  unit VARCHAR(32) NOT NULL DEFAULT 'm',
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_work_process_name_active ON work_process(name) WHERE deleted = 0;

CREATE TABLE team (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  leader_name VARCHAR(64),
  phone VARCHAR(32),
  work_type VARCHAR(64),
  settlement_method VARCHAR(32) NOT NULL DEFAULT 'quantity',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_name ON team(name);

CREATE TABLE team_unit_price (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  work_process_id BIGINT NOT NULL,
  unit VARCHAR(32) NOT NULL,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  effective_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_unit_price_lookup ON team_unit_price(project_id, team_id, work_process_id);

CREATE TABLE project_process_price (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  work_process_id BIGINT NOT NULL,
  unit VARCHAR(32) NOT NULL,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  effective_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_process_price_lookup ON project_process_price(project_id, work_process_id);

CREATE TABLE daily_work_report (
  id BIGINT PRIMARY KEY,
  work_date DATE NOT NULL,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT NOT NULL,
  work_process_id BIGINT NOT NULL,
  completed_quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit VARCHAR(32) NOT NULL,
  team_id BIGINT,
  worker_count INTEGER NOT NULL DEFAULT 0,
  machinery_usage VARCHAR(255),
  exception_note TEXT,
  reporter_name VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  rejected_reason VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_work_report_project_date ON daily_work_report(project_id, work_date);
CREATE INDEX idx_daily_work_report_status ON daily_work_report(status);
CREATE INDEX idx_daily_work_report_settlement ON daily_work_report(project_id, team_id, work_date, status);

CREATE TABLE daily_work_report_file (
  id BIGINT PRIMARY KEY,
  daily_work_report_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  file_type VARCHAR(32) NOT NULL DEFAULT 'photo',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE work_quantity_confirm (
  id BIGINT PRIMARY KEY,
  daily_work_report_id BIGINT NOT NULL,
  confirm_stage VARCHAR(32) NOT NULL,
  confirm_status VARCHAR(32) NOT NULL,
  confirmer_id BIGINT,
  confirmer_name VARCHAR(64),
  remark VARCHAR(255),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_settlement (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  settlement_month VARCHAR(7) NOT NULL,
  confirmed_quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  settlement_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  compensation_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  actual_pay_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  settlement_status VARCHAR(32) NOT NULL DEFAULT 'draft',
  payment_status VARCHAR(32) NOT NULL DEFAULT 'unpaid',
  paid_date DATE,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_settlement_lookup ON team_settlement(project_id, team_id, settlement_month);

CREATE TABLE team_settlement_detail (
  id BIGINT PRIMARY KEY,
  team_settlement_id BIGINT NOT NULL,
  daily_work_report_id BIGINT NOT NULL,
  work_process_id BIGINT NOT NULL,
  quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit VARCHAR(32) NOT NULL,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255)
);

CREATE UNIQUE INDEX uk_settlement_detail_report ON team_settlement_detail(daily_work_report_id);

CREATE TABLE variation_order (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  project_section_id BIGINT,
  occurrence_date DATE NOT NULL,
  variation_type VARCHAR(64) NOT NULL,
  reason TEXT,
  responsible_party VARCHAR(64),
  increased_quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  estimated_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  labor_count INTEGER NOT NULL DEFAULT 0,
  machinery_shift VARCHAR(120),
  material_note TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  rejected_reason VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_variation_order_project_status ON variation_order(project_id, status);

CREATE TABLE variation_order_file (
  id BIGINT PRIMARY KEY,
  variation_order_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  file_type VARCHAR(32) NOT NULL DEFAULT 'evidence',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_cost_record (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  cost_date DATE NOT NULL,
  cost_type VARCHAR(64) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  description VARCHAR(255),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_cost_record_project ON project_cost_record(project_id, cost_date);

INSERT INTO work_process (id, name, unit, sort_order, status)
VALUES
  (3001, '测量放线', 'm', 10, 'active'),
  (3002, '沟槽开挖', 'm3', 20, 'active'),
  (3003, '基底处理', 'm2', 30, 'active'),
  (3004, '垫层施工', 'm3', 40, 'active'),
  (3005, '底板钢筋', 't', 50, 'active'),
  (3006, '底板模板', 'm2', 60, 'active'),
  (3007, '底板混凝土', 'm3', 70, 'active'),
  (3008, '侧墙钢筋', 't', 80, 'active'),
  (3009, '侧墙模板', 'm2', 90, 'active'),
  (3010, '侧墙混凝土', 'm3', 100, 'active'),
  (3011, '顶板施工', 'm3', 110, 'active'),
  (3012, '防水施工', 'm2', 120, 'active'),
  (3013, '回填', 'm3', 130, 'active'),
  (3014, '支架安装', '套', 140, 'active'),
  (3015, '接地施工', 'm', 150, 'active'),
  (3016, '防火封堵', '处', 160, 'active'),
  (3017, '清理验收', '段', 170, 'active');
