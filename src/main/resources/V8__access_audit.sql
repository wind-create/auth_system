CREATE TABLE IF NOT EXISTS access_audit (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at  timestamptz NOT NULL DEFAULT now(),
  user_id      uuid REFERENCES user_account(id) ON DELETE SET NULL,
  merchant_id  uuid REFERENCES merchant(id) ON DELETE SET NULL,
  http_method  text,
  path         text,
  allowed      boolean NOT NULL,
  required_perm text,
  client_ip    text,
  user_agent   text,
  token_jti    text,
  note         text
);

CREATE INDEX IF NOT EXISTS idx_access_audit_time     ON access_audit(occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_audit_user     ON access_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_access_audit_merchant ON access_audit(merchant_id);
