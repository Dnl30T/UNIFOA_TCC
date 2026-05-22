-- Add manager_id, counselor_id, and team_code to teams table
ALTER TABLE teams
    ADD COLUMN manager_id    UUID NOT NULL,
    ADD COLUMN counselor_id  UUID,
    ADD COLUMN team_code     VARCHAR(8) NOT NULL UNIQUE,
    ADD COLUMN created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD CONSTRAINT fk_team_manager FOREIGN KEY (manager_id) REFERENCES app_users(id);

-- Create team_memberships table (employees in teams)
CREATE TABLE team_memberships (
    id          UUID PRIMARY KEY,
    team_id     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_team_employee UNIQUE (team_id, employee_id)
);

-- Index for quick lookups
CREATE INDEX idx_team_memberships_employee_id ON team_memberships(employee_id);
CREATE INDEX idx_team_memberships_team_id ON team_memberships(team_id);
CREATE INDEX idx_teams_manager_id ON teams(manager_id);
CREATE INDEX idx_teams_counselor_id ON teams(counselor_id);
CREATE INDEX idx_teams_team_code ON teams(team_code);
