CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS api_key (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  key_prefix    text NOT NULL UNIQUE,      -- contoh: 16 char pertama full key
  key_hash      text NOT NULL,             -- hash dari full key (mis. SHA-256 base64)

  name          text NOT NULL,             -- untuk manusia: "Prod key #1"
  description   text,

  owner_user_id uuid REFERENCES user_account(id),
  merchant_id   uuid REFERENCES merchant(id),

  scopes        jsonb NOT NULL DEFAULT '[]'::jsonb,  -- ["invoice.read_org", ...]
  is_active     boolean NOT NULL DEFAULT true,

  created_at    timestamptz NOT NULL DEFAULT now(),
  created_by    uuid REFERENCES user_account(id),

  last_used_at  timestamptz,
  expires_at    timestamptz,
  revoked_at    timestamptz,

  CONSTRAINT chk_scopes_array CHECK (jsonb_typeof(scopes) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_api_key_merchant ON api_key(merchant_id);
CREATE INDEX IF NOT EXISTS idx_api_key_owner ON api_key(owner_user_id);
