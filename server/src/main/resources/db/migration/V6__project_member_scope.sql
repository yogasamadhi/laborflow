CREATE TABLE project_member (
  id BIGINT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  user_id BIGINT,
  employee_id BIGINT,
  role_name VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_member_project ON project_member(project_id);
CREATE INDEX idx_project_member_user ON project_member(user_id) WHERE deleted = 0;
CREATE INDEX idx_project_member_employee ON project_member(employee_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_project_member_user_active ON project_member(project_id, user_id) WHERE deleted = 0 AND user_id IS NOT NULL;
CREATE UNIQUE INDEX uk_project_member_employee_active ON project_member(project_id, employee_id) WHERE deleted = 0 AND employee_id IS NOT NULL;
