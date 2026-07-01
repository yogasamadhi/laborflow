ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS failed_login_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(64);

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS password_updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE sys_user
SET password_updated_at = COALESCE(password_updated_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE password_updated_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sys_user_locked_until
  ON sys_user(locked_until)
  WHERE locked_until IS NOT NULL;
