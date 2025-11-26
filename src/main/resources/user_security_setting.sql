CREATE TABLE IF NOT EXISTS user_security_setting (
  user_id           uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
  mfa_totp_enabled  boolean NOT NULL DEFAULT false,
  mfa_updated_at    timestamptz
);

CREATE TABLE IF NOT EXISTS totp_credential (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  -- secret dalam bentuk encrypted/base32, JANGAN plaintext
  secret_enc   text NOT NULL,
  issuer       text NOT NULL,
  account_name text NOT NULL, -- biasanya email
  created_at   timestamptz NOT NULL DEFAULT now(),
  verified_at  timestamptz,
  last_used_at timestamptz,
  is_active    boolean NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_totp_credential_user_active
  ON totp_credential(user_id)
  WHERE is_active = true;
