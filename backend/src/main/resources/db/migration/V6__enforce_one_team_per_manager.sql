-- Enforce at the database level that each manager owns at most one team.
-- This complements the application-layer check in TeamService.registerTeam.
ALTER TABLE teams ADD CONSTRAINT uk_teams_manager_id UNIQUE (manager_id);
