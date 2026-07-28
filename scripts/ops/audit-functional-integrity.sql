-- LaborFlow functional-integrity audit.
-- Read-only: every statement is a SELECT and returns only technical IDs,
-- statuses, dates and aggregate amounts. Run against a backup or non-production
-- clone first and archive the output as restricted Gate evidence.

-- 1. Salary detail formula mismatches.
SELECT detail.salary_monthly_id,
       monthly.salary_month,
       monthly.status AS monthly_status,
       COUNT(*) AS mismatch_detail_count
FROM salary_monthly_detail detail
JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
WHERE detail.deleted = 0
  AND monthly.deleted = 0
  AND detail.actual_amount <> detail.payable_amount - detail.deduction_amount
GROUP BY detail.salary_monthly_id, monthly.salary_month, monthly.status
ORDER BY monthly.salary_month, detail.salary_monthly_id;

-- 2. Draft rows whose deduction exceeds payable. V23 intentionally does not
-- repair these rows automatically.
SELECT detail.id AS salary_detail_id,
       detail.salary_monthly_id,
       monthly.salary_month,
       detail.payable_amount,
       detail.deduction_amount,
       detail.actual_amount
FROM salary_monthly_detail detail
JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
WHERE detail.deleted = 0
  AND monthly.deleted = 0
  AND monthly.status = 'draft'
  AND detail.deduction_amount > detail.payable_amount
ORDER BY monthly.salary_month, detail.id;

-- 3. Salary header/detail count or total mismatches, including empty submitted payrolls.
WITH detail_summary AS (
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
  GROUP BY monthly.id
)
SELECT monthly.id AS salary_monthly_id,
       monthly.salary_month,
       monthly.status,
       monthly.total_employee_count,
       summary.employee_count AS detail_employee_count,
       monthly.total_payable_amount,
       summary.payable_amount AS detail_payable_amount,
       monthly.total_deduction_amount,
       summary.deduction_amount AS detail_deduction_amount,
       monthly.total_actual_amount,
       summary.actual_amount AS detail_actual_amount
FROM salary_monthly monthly
JOIN detail_summary summary ON summary.id = monthly.id
WHERE monthly.deleted = 0
  AND (
    monthly.total_employee_count <> summary.employee_count
    OR monthly.total_payable_amount <> summary.payable_amount
    OR monthly.total_deduction_amount <> summary.deduction_amount
    OR monthly.total_actual_amount <> summary.actual_amount
    OR monthly.status IN ('pending_approval', 'approved', 'paid') AND summary.employee_count = 0
  )
ORDER BY monthly.salary_month, monthly.id;

-- 4. Payroll rows for inactive/deleted employees or without a profile effective for the payroll month.
SELECT detail.id AS salary_detail_id,
       monthly.id AS salary_monthly_id,
       monthly.salary_month,
       monthly.status AS monthly_status,
       employee.id AS employee_id,
       employee.status AS employee_status,
       employee.deleted AS employee_deleted,
       CASE WHEN EXISTS (
         SELECT 1
         FROM salary_profile profile
         WHERE profile.employee_id = employee.id
           AND profile.deleted = 0
           AND profile.status = 'active'
           AND (profile.effective_date IS NULL
             OR profile.effective_date <= (to_date(monthly.salary_month || '-01', 'YYYY-MM-DD')
               + INTERVAL '1 month - 1 day')::DATE)
       ) THEN 'eligible_profile_exists' ELSE 'no_eligible_profile' END AS profile_check
FROM salary_monthly_detail detail
JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
JOIN employee ON employee.id = detail.employee_id
WHERE detail.deleted = 0
  AND monthly.deleted = 0
  AND (
    employee.deleted <> 0
    OR employee.status <> 'active'
    OR NOT EXISTS (
      SELECT 1
      FROM salary_profile profile
      WHERE profile.employee_id = employee.id
        AND profile.deleted = 0
        AND profile.status = 'active'
        AND (profile.effective_date IS NULL
          OR profile.effective_date <= (to_date(monthly.salary_month || '-01', 'YYYY-MM-DD')
            + INTERVAL '1 month - 1 day')::DATE)
    )
  )
ORDER BY monthly.salary_month, detail.id;

-- 5. Source snapshots that explicitly identify a future-effective profile.
WITH source_profile AS (
  SELECT source.id AS source_record_id,
         source.salary_detail_id,
         substring(source.remark FROM '工资档案ID:([0-9]+)')::BIGINT AS profile_id
  FROM salary_source_record source
  WHERE source.remark ~ '工资档案ID:[0-9]+'
)
SELECT source.source_record_id,
       detail.id AS salary_detail_id,
       monthly.id AS salary_monthly_id,
       monthly.salary_month,
       monthly.status AS monthly_status,
       source.profile_id,
       profile.effective_date
FROM source_profile source
JOIN salary_monthly_detail detail ON detail.id = source.salary_detail_id
JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
JOIN salary_profile profile ON profile.id = source.profile_id
WHERE detail.deleted = 0
  AND monthly.deleted = 0
  AND profile.effective_date >
      (to_date(monthly.salary_month || '-01', 'YYYY-MM-DD') + INTERVAL '1 month - 1 day')::DATE
ORDER BY monthly.salary_month, detail.id, source.source_record_id;

-- 5b. Legacy payroll rows without the V23 profile snapshot that had a future
-- profile available when the detail was generated. These are candidates, not
-- proof; compare their fixed-salary source rows with the profile items.
SELECT detail.id AS salary_detail_id,
       monthly.id AS salary_monthly_id,
       monthly.salary_month,
       monthly.status AS monthly_status,
       profile.id AS candidate_future_profile_id,
       profile.effective_date,
       profile.created_at AS profile_created_at,
       detail.created_at AS detail_created_at
FROM salary_monthly_detail detail
JOIN salary_monthly monthly ON monthly.id = detail.salary_monthly_id
JOIN salary_profile profile
  ON profile.employee_id = detail.employee_id
 AND profile.deleted = 0
 AND profile.status = 'active'
 AND profile.created_at <= detail.created_at
WHERE detail.deleted = 0
  AND monthly.deleted = 0
  AND profile.effective_date >
      (to_date(monthly.salary_month || '-01', 'YYYY-MM-DD') + INTERVAL '1 month - 1 day')::DATE
  AND NOT EXISTS (
    SELECT 1
    FROM salary_source_record source
    WHERE source.salary_detail_id = detail.id
      AND source.remark ~ '工资档案ID:[0-9]+'
  )
ORDER BY monthly.salary_month, detail.id, profile.effective_date, profile.id;

-- 6. Confirmed allocations attached to an unreleased bonus pool.
SELECT allocation.id AS allocation_id,
       allocation.bonus_pool_id,
       allocation.project_id,
       allocation.employee_id,
       allocation.payment_month,
       allocation.status AS allocation_status,
       pool.release_status AS pool_status,
       allocation.allocation_amount
FROM project_bonus_allocation allocation
JOIN project_bonus_pool pool ON pool.id = allocation.bonus_pool_id
WHERE allocation.deleted = 0
  AND allocation.status = 'confirmed'
  AND (pool.deleted <> 0 OR pool.release_status <> 'released')
ORDER BY allocation.payment_month, allocation.id;

-- 7. Cross-project section links.
SELECT *
FROM (
  SELECT 'daily_work_report' AS source_table, row.id AS source_id, row.project_id,
         row.project_section_id, section.project_id AS section_project_id
  FROM daily_work_report row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
  UNION ALL
  SELECT 'variation_order', row.id, row.project_id, row.project_section_id, section.project_id
  FROM variation_order row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
  UNION ALL
  SELECT 'project_contract_item', row.id, row.project_id, row.project_section_id, section.project_id
  FROM project_contract_item row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
  UNION ALL
  SELECT 'machinery_work_record', row.id, row.project_id, row.project_section_id, section.project_id
  FROM machinery_work_record row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
  UNION ALL
  SELECT 'material_record', row.id, row.project_id, row.project_section_id, section.project_id
  FROM material_record row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
  UNION ALL
  SELECT 'project_document_archive', row.id, row.project_id, row.project_section_id, section.project_id
  FROM project_document_archive row
  JOIN project_section section ON section.id = row.project_section_id
  WHERE row.deleted = 0 AND row.project_id <> section.project_id
) mismatch
ORDER BY source_table, source_id;

-- 8. Daily reports whose recorded project-process price was not effective on the work date.
SELECT report.id AS daily_report_id,
       report.project_id,
       report.work_date,
       price.id AS selected_price_id,
       price.effective_date,
       report.output_value
FROM daily_work_report report
JOIN project_process_price price
  ON price.project_id = report.project_id
 AND price.work_process_id = report.work_process_id
 AND report.pricing_basis LIKE '%项目工序产值单价ID:' || price.id || ';%'
WHERE report.deleted = 0
  AND price.effective_date > report.work_date
ORDER BY report.work_date, report.id;

-- 9. Settlement lines whose recorded team price was not effective on the source work date.
SELECT detail.id AS settlement_detail_id,
       detail.team_settlement_id,
       detail.daily_work_report_id,
       detail.work_date,
       price.id AS selected_price_id,
       price.effective_date,
       detail.amount
FROM team_settlement_detail detail
JOIN team_unit_price price
  ON detail.source_snapshot LIKE '%班组单价ID:' || price.id
WHERE price.effective_date > detail.work_date
ORDER BY detail.work_date, detail.id;
