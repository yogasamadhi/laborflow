ALTER TABLE salary_monthly
  ADD COLUMN include_fixed_salary INTEGER NOT NULL DEFAULT 1,
  ADD COLUMN include_attendance_deduction INTEGER NOT NULL DEFAULT 1,
  ADD COLUMN include_performance_salary INTEGER NOT NULL DEFAULT 1,
  ADD COLUMN include_project_bonus INTEGER NOT NULL DEFAULT 1;

ALTER TABLE salary_monthly
  ADD CONSTRAINT ck_salary_monthly_generation_rules CHECK (
    include_fixed_salary IN (0, 1)
    AND include_attendance_deduction IN (0, 1)
    AND include_performance_salary IN (0, 1)
    AND include_project_bonus IN (0, 1)
  ) NOT VALID;
