-- Tracks revoked JWTs by their JTI so that /api/auth/logout can invalidate a
-- token before its natural expiry. Rows whose expires_at has passed are safe
-- to purge periodically (the JwtAuthenticationFilter treats expired tokens as
-- invalid regardless).
CREATE TABLE token_blacklist (
    jti         VARCHAR(64)  PRIMARY KEY,
    user_id     BIGINT,
    expires_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked_by  BIGINT,
    revoked_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_token_blacklist_expires_at
  ON token_blacklist(expires_at)
  WHERE deleted = 0;
