ALTER TABLE salary_monthly
  ADD COLUMN department_id BIGINT;

WITH department_candidates AS (
  SELECT salary_monthly_id,
         MIN(department_id) AS department_id
  FROM salary_monthly_detail
  WHERE deleted = 0
    AND department_id IS NOT NULL
  GROUP BY salary_monthly_id
  HAVING COUNT(DISTINCT department_id) = 1
)
UPDATE salary_monthly monthly
SET department_id = candidate.department_id,
    updated_at = CURRENT_TIMESTAMP
FROM department_candidates candidate
WHERE candidate.salary_monthly_id = monthly.id;

DROP INDEX IF EXISTS uk_salary_monthly_month;

CREATE UNIQUE INDEX uk_salary_monthly_month_department
  ON salary_monthly(salary_month, department_id)
  WHERE deleted = 0 AND department_id IS NOT NULL;

CREATE INDEX idx_salary_monthly_department
  ON salary_monthly(department_id, salary_month)
  WHERE deleted = 0;

ALTER TABLE salary_monthly
  ADD CONSTRAINT fk_salary_monthly_department
  FOREIGN KEY (department_id) REFERENCES department(id) NOT VALID;
