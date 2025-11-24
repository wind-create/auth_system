ALTER TABLE user_account
  ADD COLUMN IF NOT EXISTS auth_state_version integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS auth_state_changed_at timestamptz NOT NULL DEFAULT now();

-- Optional: fungsi helper untuk bump
CREATE OR REPLACE FUNCTION bump_auth_state_version(p_user_id uuid)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_account
  SET auth_state_version = auth_state_version + 1,
      auth_state_changed_at = now()
  WHERE id = p_user_id;
END$$;
