-- =========================================================
-- Tahap 8 - Multi Application
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Master application
--    Mewakili produk/aplikasi:
--    - MINIPSP (Mini PSP)
--    - JASTIP (Jastiper)
--    - POS (Kasir)
--    - AUTH (Auth Admin Console / panel admin)
CREATE TABLE IF NOT EXISTS application (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code       text NOT NULL UNIQUE,   -- "MINIPSP", "JASTIP", "POS", "AUTH"
  name       text NOT NULL,
  is_system  boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Seed beberapa aplikasi awal
INSERT INTO application (code, name, is_system) VALUES
  ('MINIPSP', 'Mini PSP QRIS', false),
  ('JASTIP',  'Jastiper Platform', false),
  ('POS',     'POS / Kasir', false),
  ('AUTH',    'Auth Admin Console', true)
ON CONFLICT (code) DO NOTHING;


-- 2) Relasi user ↔ role ↔ application
--    → role bisa bersifat global (user_role)
--      atau per-application (user_application_role)
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


-- 3) Tandai session milik aplikasi mana
--    → supaya saat refresh kita tahu ini session untuk app apa
ALTER TABLE session
  ADD COLUMN IF NOT EXISTS application_id uuid REFERENCES application(id);

-- Inisialisasi session lama:
-- Untuk session yang belum punya application_id,
-- set ke AUTH (anggap semua session lama itu dari "Auth Console" / default).
DO $$
DECLARE
  v_auth_app_id uuid;
BEGIN
  SELECT id INTO v_auth_app_id FROM application WHERE code = 'AUTH';
  IF v_auth_app_id IS NOT NULL THEN
    UPDATE session s
    SET application_id = v_auth_app_id
    WHERE s.application_id IS NULL;
  END IF;
END $$;


-- 4) (Opsional tapi disarankan) Tandai API key milik aplikasi mana
--    → supaya API key JASTIP tidak bisa dipakai di MINIPSP, dsb.
ALTER TABLE api_key
  ADD COLUMN IF NOT EXISTS application_id uuid REFERENCES application(id);

-- Kalau mau, bisa inisialisasi default ke NULL dulu (artinya "global"),
-- nanti pelan-pelan diisi via UI/admin.
