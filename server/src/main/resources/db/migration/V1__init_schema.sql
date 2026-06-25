CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  employee_id BIGINT,
  nickname VARCHAR(64),
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);

CREATE TABLE department (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE position (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  department_id BIGINT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee (
  id BIGINT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  id_card VARCHAR(32),
  gender VARCHAR(16),
  birth_date DATE,
  department_id BIGINT,
  position_id BIGINT,
  employee_type VARCHAR(32) NOT NULL DEFAULT 'company_employee',
  entry_date DATE,
  leave_date DATE,
  bank_account VARCHAR(64),
  bank_name VARCHAR(128),
  emergency_contact VARCHAR(64),
  emergency_phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_name ON employee(name);
CREATE INDEX idx_employee_phone ON employee(phone);
CREATE INDEX idx_employee_department_id ON employee(department_id);
CREATE INDEX idx_employee_position_id ON employee(position_id);
CREATE UNIQUE INDEX uk_employee_phone_active ON employee(phone) WHERE deleted = 0 AND phone IS NOT NULL AND phone <> '';

CREATE TABLE salary_item (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(32) NOT NULL,
  is_fixed SMALLINT NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_profile (
  id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  effective_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_profile_item (
  id BIGINT PRIMARY KEY,
  salary_profile_id BIGINT NOT NULL,
  salary_item_id BIGINT,
  salary_item_name VARCHAR(100) NOT NULL,
  salary_item_type VARCHAR(32) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255)
);

CREATE TABLE salary_monthly (
  id BIGINT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL,
  title VARCHAR(100) NOT NULL,
  total_employee_count INTEGER NOT NULL DEFAULT 0,
  total_payable_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_actual_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  created_by BIGINT,
  approved_by BIGINT,
  approved_at TIMESTAMP WITHOUT TIME ZONE,
  paid_at TIMESTAMP WITHOUT TIME ZONE,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_salary_monthly_month ON salary_monthly(salary_month) WHERE deleted = 0;

CREATE TABLE salary_monthly_detail (
  id BIGINT PRIMARY KEY,
  salary_monthly_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  department_id BIGINT,
  position_id BIGINT,
  project_id BIGINT,
  payable_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  actual_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  payment_status VARCHAR(32) NOT NULL DEFAULT 'unpaid',
  payment_date DATE,
  payment_method VARCHAR(32),
  payment_proof_url VARCHAR(255),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_detail_item (
  id BIGINT PRIMARY KEY,
  salary_detail_id BIGINT NOT NULL,
  salary_item_id BIGINT,
  salary_item_name VARCHAR(100) NOT NULL,
  salary_item_type VARCHAR(32) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255)
);

CREATE TABLE file_record (
  id BIGINT PRIMARY KEY,
  original_name VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  content_type VARCHAR(100),
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY,
  module VARCHAR(64) NOT NULL,
  business_id BIGINT,
  action VARCHAR(64) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  content TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

