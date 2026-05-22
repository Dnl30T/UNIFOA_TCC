-- Add optional profile fields to app_users
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS name                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone_number        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS company             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS job_title           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(512);

-- Email is now the primary login identifier and must be unique
ALTER TABLE app_users ADD CONSTRAINT uk_app_users_email UNIQUE (email);
