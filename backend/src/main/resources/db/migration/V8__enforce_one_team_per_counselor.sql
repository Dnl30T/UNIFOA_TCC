-- Enforce at the database level that each counselor is assigned to at most one team.
-- This complements the application-layer check in TeamService.assignCounselor.
ALTER TABLE teams ADD CONSTRAINT uk_teams_counselor_id UNIQUE (counselor_id);
