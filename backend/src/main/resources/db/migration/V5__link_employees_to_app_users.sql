-- Link employees to app_users and remove email redundancy
ALTER TABLE employees
    DROP CONSTRAINT uk_employee_email,
    DROP COLUMN email,
    ADD COLUMN app_user_id UUID NOT NULL UNIQUE,
    ADD CONSTRAINT fk_employee_app_user FOREIGN KEY (app_user_id) REFERENCES app_users(id) ON DELETE CASCADE;

-- Index for quick lookups
CREATE INDEX idx_employees_app_user_id ON employees(app_user_id);
