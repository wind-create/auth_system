CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- master tables
CREATE TABLE IF NOT EXISTS role (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code       text NOT NULL UNIQUE,
  name       text NOT NULL,
  is_system  boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS permission (
  id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  name text NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permission (
  role_id uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  perm_id uuid NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
  PRIMARY KEY (role_id, perm_id)
);

CREATE TABLE IF NOT EXISTS user_role (
  user_id    uuid NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
  role_id    uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  expires_at timestamptz,
  granted_by uuid REFERENCES user_account(id),
  note       text,
  PRIMARY KEY (user_id, role_id)
);

-- seed global permissions + admin role
INSERT INTO permission (code, name) VALUES
  ('user.read_any','Read users (global)'),
  ('user.write_any','Write users (global)'),
  ('role.manage','Manage roles & permissions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role (code,name,is_system) VALUES
  ('admin','Administrator',true)
ON CONFLICT (code) DO NOTHING;

-- grant admin → all current permissions
INSERT INTO role_permission (role_id, perm_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code='admin'
ON CONFLICT DO NOTHING;


-- self-scope permissions for every user
INSERT INTO permission (code, name) VALUES
 ('profile.read_self','Read own profile'),
 ('profile.update_self','Update own profile'),
 ('session.read_self','Read own sessions'),
 ('session.revoke_self','Revoke own sessions'),
 ('password.change_self','Change own password')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role (code,name,is_system) VALUES
 ('basic_user','Basic User',true)
ON CONFLICT (code) DO NOTHING;

-- link self-perms to basic_user
INSERT INTO role_permission (role_id, perm_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN
 ('profile.read_self','profile.update_self','session.read_self','session.revoke_self','password.change_self')
WHERE r.code='basic_user'
ON CONFLICT DO NOTHING;

-- assign basic_user to all users that still have no role
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user_account u
CROSS JOIN (SELECT id FROM role WHERE code='basic_user') r
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.id)
ON CONFLICT DO NOTHING;
