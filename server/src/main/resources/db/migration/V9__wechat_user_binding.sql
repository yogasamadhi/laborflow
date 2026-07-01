CREATE TABLE IF NOT EXISTS wechat_user_binding (
  id BIGINT PRIMARY KEY,
  openid VARCHAR(128) NOT NULL,
  unionid VARCHAR(128),
  user_id BIGINT,
  employee_id BIGINT,
  nickname VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  last_session_at TIMESTAMP WITHOUT TIME ZONE,
  last_login_at TIMESTAMP WITHOUT TIME ZONE,
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wechat_user_binding_openid_active
  ON wechat_user_binding(openid)
  WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_wechat_user_binding_user_id
  ON wechat_user_binding(user_id)
  WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_wechat_user_binding_employee_id
  ON wechat_user_binding(employee_id)
  WHERE employee_id IS NOT NULL;
