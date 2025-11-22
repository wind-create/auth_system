CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Kolom verifikasi di user
ALTER TABLE public.user_account
  ADD COLUMN IF NOT EXISTS email_verified_at timestamptz;

-- 2) Tabel token verifikasi email (id + secret hashed)
CREATE TABLE IF NOT EXISTS public.email_verification_token (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES public.user_account(id) ON DELETE CASCADE,
  token_hash  text NOT NULL,
  expires_at  timestamptz NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  used_at     timestamptz
);

-- Satu token aktif per user (opsional, bisa diganti partial unique)
CREATE UNIQUE INDEX IF NOT EXISTS uq_email_verif_active_one
ON public.email_verification_token (user_id)
WHERE used_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_email_verif_exp ON public.email_verification_token (expires_at);

-- 3) Tabel token reset password
CREATE TABLE IF NOT EXISTS public.password_reset_token (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES public.user_account(id) ON DELETE CASCADE,
  token_hash  text NOT NULL,
  expires_at  timestamptz NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  used_at     timestamptz
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_user_active
  ON public.password_reset_token (user_id) WHERE used_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_pwd_reset_exp ON public.password_reset_token (expires_at);
