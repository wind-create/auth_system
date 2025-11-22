CREATE TABLE IF NOT EXISTS merchant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE,
  name text NOT NULL,
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

-- permission org-scoped (minimal)
INSERT INTO permission (code, name) VALUES
 ('invoice.read_org','Read invoices (own merchant)'),
 ('settlement.view_org','View settlements (own merchant)')
ON CONFLICT (code) DO NOTHING;

-- role merchant_owner (minimal)
INSERT INTO role (code,name,is_system)
VALUES ('merchant_owner','Merchant Owner', true)
ON CONFLICT (code) DO NOTHING;

-- grant perms org ke merchant_owner
INSERT INTO role_permission(role_id, perm_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('invoice.read_org','settlement.view_org')
WHERE r.code='merchant_owner'
ON CONFLICT DO NOTHING;

-- indeks bantu
CREATE INDEX IF NOT EXISTS idx_umr_user ON user_merchant_role(user_id);
CREATE INDEX IF NOT EXISTS idx_umr_merchant ON user_merchant_role(merchant_id);
