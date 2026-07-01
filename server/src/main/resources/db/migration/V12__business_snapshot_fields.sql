ALTER TABLE team_settlement
  ADD COLUMN project_name VARCHAR(120),
  ADD COLUMN team_name VARCHAR(120);

ALTER TABLE team_settlement_detail
  ADD COLUMN work_date DATE,
  ADD COLUMN work_process_name VARCHAR(120),
  ADD COLUMN source_snapshot TEXT;

ALTER TABLE project_bonus_pool
  ADD COLUMN dashboard_snapshot TEXT;
