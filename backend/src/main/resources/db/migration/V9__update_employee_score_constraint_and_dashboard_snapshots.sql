ALTER TABLE employee_results DROP CONSTRAINT IF EXISTS employee_results_score_check;
ALTER TABLE employee_results
    ADD CONSTRAINT employee_results_score_check CHECK (score >= 0);

CREATE TABLE IF NOT EXISTS dashboard_snapshots (
    id UUID PRIMARY KEY,
    role VARCHAR(24) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    team_id UUID,
    form_id UUID,
    payload_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dashboard_snapshots_role_period
    ON dashboard_snapshots (role, period_start, period_end);
