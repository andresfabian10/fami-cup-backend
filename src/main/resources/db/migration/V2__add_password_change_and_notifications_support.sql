ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;

UPDATE users
SET password_changed_at = COALESCE(password_changed_at, updated_at)
WHERE must_change_password = FALSE;
