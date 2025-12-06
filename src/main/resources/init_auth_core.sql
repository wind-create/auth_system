-- ============================================
-- INIT AUTH CORE SCHEMA (fresh DB)
-- ============================================

-- Extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Helper function: auto update updated_at
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Enum untuk status session
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname='session_status') THEN
    CREATE TYPE session_status AS ENUM ('active','revoked','expired');
  END IF;
END$$;

-- ============================================
-- APPLICATION (multi-application: MINIPSP, JASTIP, POS, AUTH)
-- ============================================

CREATE TABLE IF NOT EXISTS application (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code       text NOT NULL UNIQUE,   -- "MINIPSP", "JASTIP", "POS", "AUTH"
  name       text NOT NULL,
  is_system  boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Seed aplikasi
INSERT INTO application (code, name, is_system) VALUES
  ('MINIPSP', 'Mini PSP QRIS', false),
  ('JASTIP',  'Jastiper Platform', false),
  ('POS',     'POS / Kasir', false),
  ('AUTH',    'Auth Admin Console', true)
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- USER ACCOUNT
-- ============================================

CREATE TABLE IF NOT EXISTS user_account (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email                text NOT NULL,
  email_normalized     text GENERATED ALWAYS AS (lower(trim(email))) STORED,
  password_hash        text NOT NULL,
  full_name            text,
  email_verified_at    timestamptz,
  auth_state_version   integer NOT NULL DEFAULT 0,
  auth_state_changed_at timestamptz NOT NULL DEFAULT now(),
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_user_email UNIQUE (email_normalized)
);

DROP TRIGGER IF EXISTS tg_user_updated_at ON user_account;
CREATE TRIGGER tg_user_updated_at
BEFORE UPDATE ON user_account
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Helper untuk bump auth_state_version (revocation global)
CREATE OR REPLACE FUNCTION bump_auth_state_version(p_user_id uuid)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_account
  SET auth_state_version = auth_state_version + 1,
      auth_state_changed_at = now()
  WHERE id = p_user_id;
END$$;

-- ============================================
-- RBAC CORE: PERMISSION / ROLE / ROLE_PERMISSION / USER_ROLE
-- ============================================

CREATE TABLE IF NOT EXISTS permission (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code        text NOT NULL UNIQUE,   -- "user.read_any", "role.manage", ...
  name        text NOT NULL,
  description text,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS role (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code       text NOT NULL UNIQUE,    -- "super_admin", "merchant_owner", ...
  name       text NOT NULL,
  is_system  boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS role_permission (
  id      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  role_id uuid NOT NULL REFERENCES role(id)       ON DELETE CASCADE,
  perm_id uuid NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT uq_role_perm UNIQUE (role_id, perm_id)
);

CREATE TABLE IF NOT EXISTS user_role (
  user_id    uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  role_id    uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  expires_at timestamptz,
  granted_by uuid REFERENCES user_account(id),
  note       text,
  PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role ON user_role(role_id);

-- ============================================
-- USER_ROLE PER APPLICATION (optional Tahap 8)
-- ============================================

CREATE TABLE IF NOT EXISTS user_application_role (
  user_id        uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  application_id uuid NOT NULL REFERENCES application(id) ON DELETE CASCADE,
  role_id        uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  expires_at     timestamptz,
  granted_by     uuid REFERENCES user_account(id),
  note           text,
  PRIMARY KEY (user_id, application_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_uappl_user ON user_application_role(user_id);
CREATE INDEX IF NOT EXISTS idx_uappl_app  ON user_application_role(application_id);

-- ============================================
-- MERCHANT & USER_MERCHANT_ROLE (MiniPSP Org-scope)
-- ============================================

CREATE TABLE IF NOT EXISTS merchant (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code       text UNIQUE,
  name       text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_merchant_role (
  user_id     uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  merchant_id uuid NOT NULL REFERENCES merchant(id) ON DELETE CASCADE,
  role_id     uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  expires_at  timestamptz,
  granted_by  uuid REFERENCES user_account(id),
  note        text,
  scope       jsonb NOT NULL DEFAULT '{}'::jsonb,
  PRIMARY KEY (user_id, merchant_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_umr_user     ON user_merchant_role(user_id);
CREATE INDEX IF NOT EXISTS idx_umr_merchant ON user_merchant_role(merchant_id);

-- ============================================
-- USER SECURITY (MFA TOTP)
-- ============================================

CREATE TABLE IF NOT EXISTS user_security_setting (
  user_id          uuid PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
  mfa_totp_enabled boolean NOT NULL DEFAULT false,
  mfa_updated_at   timestamptz
);

CREATE TABLE IF NOT EXISTS totp_credential (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  secret_enc   text NOT NULL,  -- encrypted/base32, jangan plaintext
  issuer       text NOT NULL,
  account_name text NOT NULL,  -- biasanya email
  created_at   timestamptz NOT NULL DEFAULT now(),
  verified_at  timestamptz,
  last_used_at timestamptz,
  is_active    boolean NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_totp_credential_user_active
  ON totp_credential(user_id)
  WHERE is_active = true;

-- ============================================
-- SESSION (access / refresh)
-- ============================================

CREATE TABLE IF NOT EXISTS session (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id            uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  refresh_token_hash text NOT NULL,
  status             text NOT NULL DEFAULT 'active',
  ip_address         text,
  user_agent         text,
  last_rotated_at    timestamptz NOT NULL DEFAULT now(),
  expires_at         timestamptz NOT NULL,
  revoked_at         timestamptz,
  application_id     uuid REFERENCES application(id),
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_session_user    ON session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_status  ON session(status);
CREATE INDEX IF NOT EXISTS idx_session_expires ON session(expires_at);
CREATE INDEX IF NOT EXISTS idx_session_app     ON session(application_id);

DROP TRIGGER IF EXISTS tg_session_updated_at ON session;
CREATE TRIGGER tg_session_updated_at
BEFORE UPDATE ON session
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================
-- EMAIL VERIFICATION & PASSWORD RESET TOKEN
-- ============================================

CREATE TABLE IF NOT EXISTS email_verification_token (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  token_hash text NOT NULL,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  used_at    timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_email_verif_active_one
  ON email_verification_token (user_id)
  WHERE used_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_email_verif_exp
  ON email_verification_token (expires_at);

CREATE TABLE IF NOT EXISTS password_reset_token (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  token_hash text NOT NULL,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  used_at    timestamptz
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_user_active
  ON password_reset_token (user_id) WHERE used_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_pwd_reset_exp
  ON password_reset_token (expires_at);

-- ============================================
-- ACCESS AUDIT
-- ============================================

CREATE TABLE IF NOT EXISTS access_audit (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at   timestamptz NOT NULL DEFAULT now(),
  user_id       uuid REFERENCES user_account(id) ON DELETE SET NULL,
  merchant_id   uuid REFERENCES merchant(id) ON DELETE SET NULL,
  http_method   text,
  path          text,
  allowed       boolean NOT NULL,
  required_perm text,
  client_ip     text,
  user_agent    text,
  token_jti     text,
  note          text
);

CREATE INDEX IF NOT EXISTS idx_access_audit_time
  ON access_audit(occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_access_audit_user
  ON access_audit(user_id);

CREATE INDEX IF NOT EXISTS idx_access_audit_merchant
  ON access_audit(merchant_id);

-- ============================================
-- API KEY (per-merchant & per-application)
-- ============================================

CREATE TABLE IF NOT EXISTS api_key (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  key_prefix     text NOT NULL UNIQUE,      -- contoh: 16 char pertama full key
  key_hash       text NOT NULL,             -- hash dari full key (mis. SHA-256 base64)

  name           text NOT NULL,             -- untuk manusia: "Prod key #1"
  description    text,

  owner_user_id  uuid REFERENCES user_account(id),
  merchant_id    uuid REFERENCES merchant(id),
  application_id uuid REFERENCES application(id),

  scopes         jsonb NOT NULL DEFAULT '[]'::jsonb,  -- ["invoice.read_org", ...]
  is_active      boolean NOT NULL DEFAULT true,

  created_at     timestamptz NOT NULL DEFAULT now(),
  created_by     uuid REFERENCES user_account(id),

  last_used_at   timestamptz,
  expires_at     timestamptz,
  revoked_at     timestamptz,

  CONSTRAINT chk_scopes_array CHECK (jsonb_typeof(scopes) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_api_key_merchant ON api_key(merchant_id);
CREATE INDEX IF NOT EXISTS idx_api_key_owner    ON api_key(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_api_key_app      ON api_key(application_id);

-- ============================================
-- SEED PERMISSIONS & ROLES MINIMAL
-- ============================================

-- Permissions yang dipakai di kode (SecurityConfig, ScopeGuard, dll)
INSERT INTO permission (code, name, description) VALUES
 ('user.read_any',        'Read any user',          'Read all users'),
 ('user.write_any',       'Write any user',         'Create/update any user'),
 ('role.manage',          'Manage roles',           'Manage roles & permissions'),
 ('session.read_self',    'Read own sessions',      'View own sessions'),
 ('session.revoke_self',  'Revoke own sessions',    'Revoke own sessions'),
 ('profile.read_self',    'Read own profile',       'View own profile'),
 ('profile.update_self',  'Update own profile',     'Update own profile'),
 ('password.change_self', 'Change own password',    'Change own password'),
 ('audit.view',           'View access audit',      'View audit log'),
 ('api_key.manage',       'Manage API keys',        'Create/ revoke API keys'),
 -- org-scoped (MiniPSP)
 ('invoice.read_org',     'Read invoices (org)',    'Read invoices for own merchant'),
 ('settlement.view_org',  'View settlements (org)', 'View settlements for own merchant')
ON CONFLICT (code) DO NOTHING;

-- Roles: super_admin & merchant_owner
INSERT INTO role (code, name, is_system) VALUES
 ('super_admin',   'Super Administrator', true),
 ('merchant_owner','Merchant Owner',      true)
ON CONFLICT (code) DO NOTHING;

-- Grant semua permission ke super_admin
INSERT INTO role_permission (role_id, perm_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'super_admin'
ON CONFLICT DO NOTHING;

-- Grant org perms ke merchant_owner
INSERT INTO role_permission (role_id, perm_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('invoice.read_org','settlement.view_org')
WHERE r.code = 'merchant_owner'
ON CONFLICT DO NOTHING;



--- tes

-- Ganti email di sini dengan email user kamu
WITH u AS (
  SELECT id AS user_id
  FROM user_account
  WHERE email_normalized = lower(trim('nardowilli@gmail.com'))
),
r AS (
  SELECT id AS role_id
  FROM role
  WHERE code = 'super_admin'
)
INSERT INTO user_role (user_id, role_id, granted_by, note)
SELECT u.user_id, r.role_id, u.user_id, 'bootstrap super_admin'
FROM u, r
ON CONFLICT (user_id, role_id) DO NOTHING;
