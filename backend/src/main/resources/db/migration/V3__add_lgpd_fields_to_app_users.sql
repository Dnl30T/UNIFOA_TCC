-- Rename password → password_hash (field never held plain text, renaming for clarity)
ALTER TABLE app_users RENAME COLUMN password TO password_hash;

-- Extend username/email columns to accommodate anonymized markers (uuid length)
ALTER TABLE app_users ALTER COLUMN username TYPE VARCHAR(255);
ALTER TABLE app_users ALTER COLUMN email    TYPE VARCHAR(255);

-- LGPD consent tracking
ALTER TABLE app_users
    ADD COLUMN consent_given BOOLEAN   NOT NULL DEFAULT TRUE,
    ADD COLUMN consent_date  TIMESTAMPTZ        DEFAULT NOW();

-- Audit timestamps
ALTER TABLE app_users
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Soft-delete and anonymization support
ALTER TABLE app_users
    ADD COLUMN deleted_at  TIMESTAMPTZ          DEFAULT NULL,
    ADD COLUMN anonymized  BOOLEAN    NOT NULL  DEFAULT FALSE;
