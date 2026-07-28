-- Repair only unsubmitted draft payroll rows whose operands can produce a
-- non-negative actual amount. Submitted/approved/paid history is intentionally
-- left untouched and must be reviewed with the operational audit query.
UPDATE salary_monthly_detail detail
SET actual_amount = detail.payable_amount - detail.deduction_amount,
    updated_at = CURRENT_TIMESTAMP
FROM salary_monthly monthly
WHERE monthly.id = detail.salary_monthly_id
  AND monthly.deleted = 0
  AND monthly.status = 'draft'
  AND detail.deleted = 0
  AND detail.payable_amount >= detail.deduction_amount
  AND detail.actual_amount <> detail.payable_amount - detail.deduction_amount;

WITH draft_summary AS (
  SELECT monthly.id,
         COUNT(detail.id)::INTEGER AS employee_count,
         COALESCE(SUM(detail.payable_amount), 0) AS payable_amount,
         COALESCE(SUM(detail.deduction_amount), 0) AS deduction_amount,
         COALESCE(SUM(detail.actual_amount), 0) AS actual_amount
  FROM salary_monthly monthly
  LEFT JOIN salary_monthly_detail detail
    ON detail.salary_monthly_id = monthly.id
   AND detail.deleted = 0
  WHERE monthly.deleted = 0
    AND monthly.status = 'draft'
  GROUP BY monthly.id
)
UPDATE salary_monthly monthly
SET total_employee_count = summary.employee_count,
    total_payable_amount = summary.payable_amount,
    total_deduction_amount = summary.deduction_amount,
    total_actual_amount = summary.actual_amount,
    updated_at = CURRENT_TIMESTAMP
FROM draft_summary summary
WHERE monthly.id = summary.id;

ALTER TABLE salary_monthly_detail
  ADD CONSTRAINT ck_salary_monthly_detail_formula
  CHECK (actual_amount = payable_amount - deduction_amount) NOT VALID;

ALTER TABLE salary_monthly
  ADD CONSTRAINT ck_salary_monthly_total_formula
  CHECK (total_actual_amount = total_payable_amount - total_deduction_amount) NOT VALID;

ALTER TABLE project_section
  ADD CONSTRAINT uk_project_section_id_project UNIQUE (id, project_id);

ALTER TABLE daily_work_report
  ADD CONSTRAINT fk_daily_report_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;

ALTER TABLE variation_order
  ADD CONSTRAINT fk_variation_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;

ALTER TABLE project_contract_item
  ADD CONSTRAINT fk_contract_item_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;

ALTER TABLE machinery_work_record
  ADD CONSTRAINT fk_machinery_work_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;

ALTER TABLE material_record
  ADD CONSTRAINT fk_material_record_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;

ALTER TABLE project_document_archive
  ADD CONSTRAINT fk_project_document_section_project
  FOREIGN KEY (project_section_id, project_id)
  REFERENCES project_section(id, project_id) NOT VALID;
