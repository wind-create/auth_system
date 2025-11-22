-- V1__init_core.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN NEW.updated_at := now(); RETURN NEW; END $$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname='session_status') THEN
    CREATE TYPE session_status AS ENUM ('active','revoked','expired');
  END IF;
END$$;

CREATE TABLE IF NOT EXISTS user_account (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email             text NOT NULL,
  email_normalized  text GENERATED ALWAYS AS (lower(trim(email))) STORED,
  password_hash     text NOT NULL,
  full_name         text,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_user_email UNIQUE (email_normalized)
);
DROP TRIGGER IF EXISTS tg_user_updated_at ON user_account;
CREATE TRIGGER tg_user_updated_at BEFORE UPDATE ON user_account
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS session (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id            uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  refresh_token_hash text NOT NULL,
  status             session_status NOT NULL DEFAULT 'active',
  ip_address         text,
  user_agent         text,
  last_rotated_at    timestamptz NOT NULL DEFAULT now(),
  expires_at         timestamptz NOT NULL,
  revoked_at         timestamptz,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_session_user    ON session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_status  ON session(status);
CREATE INDEX IF NOT EXISTS idx_session_expires ON session(expires_at);
DROP TRIGGER IF EXISTS tg_session_updated_at ON session;
CREATE TRIGGER tg_session_updated_at BEFORE UPDATE ON session
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
