CREATE TABLE attendance_record (
  id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  attendance_month VARCHAR(7) NOT NULL,
  project_id BIGINT,
  work_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  leave_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  overtime_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  absent_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  field_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  project_site_days NUMERIC(12,2) NOT NULL DEFAULT 0,
  deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attendance_employee_month ON attendance_record(employee_id, attendance_month);

CREATE TABLE performance_indicator (
  id BIGINT PRIMARY KEY,
  position_id BIGINT,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(64),
  score_weight NUMERIC(12,2) NOT NULL DEFAULT 0,
  data_source VARCHAR(32) NOT NULL DEFAULT 'manual',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_indicator_position ON performance_indicator(position_id);

CREATE TABLE performance_plan (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  performance_month VARCHAR(7) NOT NULL,
  project_id BIGINT,
  position_id BIGINT,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_plan_month ON performance_plan(performance_month);

CREATE TABLE performance_score (
  id BIGINT PRIMARY KEY,
  performance_plan_id BIGINT,
  performance_month VARCHAR(7) NOT NULL,
  project_id BIGINT,
  employee_id BIGINT NOT NULL,
  position_id BIGINT,
  score NUMERIC(12,2) NOT NULL DEFAULT 0,
  performance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  auto_metric_snapshot TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_score_employee_month ON performance_score(employee_id, performance_month);

CREATE TABLE project_bonus_pool (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  bonus_month VARCHAR(7),
  final_gross_profit NUMERIC(12,2) NOT NULL DEFAULT 0,
  bonus_ratio NUMERIC(12,4) NOT NULL DEFAULT 0,
  bonus_pool_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  release_condition VARCHAR(255),
  release_status VARCHAR(32) NOT NULL DEFAULT 'draft',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_bonus_pool_project ON project_bonus_pool(project_id);

CREATE TABLE project_bonus_allocation (
  id BIGINT PRIMARY KEY,
  bonus_pool_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  role_name VARCHAR(64),
  allocation_ratio NUMERIC(12,4) NOT NULL DEFAULT 0,
  allocation_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  payment_month VARCHAR(7) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'allocated',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_bonus_allocation_employee_month ON project_bonus_allocation(employee_id, payment_month);

CREATE TABLE salary_source_record (
  id BIGINT PRIMARY KEY,
  salary_monthly_id BIGINT NOT NULL,
  salary_detail_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT,
  source_name VARCHAR(120),
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salary_source_detail ON salary_source_record(salary_detail_id);
CREATE INDEX idx_salary_source_employee ON salary_source_record(employee_id, source_type);

INSERT INTO salary_item (id, name, type, is_fixed, sort_order, status)
VALUES
  (2201, '绩效奖金', 'earning', 0, 160, 'active'),
  (2202, '项目奖金', 'earning', 0, 170, 'active'),
  (2203, '专项奖励', 'earning', 0, 180, 'active'),
  (2204, '考勤扣款', 'deduction', 0, 190, 'active')
ON CONFLICT (id) DO NOTHING;
