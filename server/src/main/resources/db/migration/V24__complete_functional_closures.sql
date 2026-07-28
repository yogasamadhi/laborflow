-- LaborFlow is not yet in production. The legacy performance data model and
-- user-only project memberships are intentionally reset instead of carrying
-- ambiguous development data into the completed workflows.

ALTER TABLE salary_item
  ADD COLUMN calculation_role VARCHAR(32) NOT NULL DEFAULT 'fixed_component';

UPDATE salary_item
SET calculation_role = 'performance_base',
    is_fixed = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2003 OR name = '绩效工资';

ALTER TABLE salary_item
  ADD CONSTRAINT ck_salary_item_calculation_role
  CHECK (calculation_role IN ('fixed_component', 'performance_base')) NOT VALID;

TRUNCATE TABLE performance_score, performance_plan, performance_indicator CASCADE;

ALTER TABLE performance_indicator
  ADD COLUMN metric_code VARCHAR(64),
  ADD COLUMN scoring_direction VARCHAR(32) NOT NULL DEFAULT 'higher_better',
  ADD COLUMN target_value NUMERIC(18,4);

ALTER TABLE performance_indicator
  ADD CONSTRAINT ck_performance_indicator_direction
  CHECK (scoring_direction IN ('higher_better', 'lower_better')) NOT VALID,
  ADD CONSTRAINT ck_performance_indicator_target
  CHECK (target_value IS NULL OR target_value >= 0) NOT VALID;

ALTER TABLE performance_plan
  DROP CONSTRAINT IF EXISTS ck_performance_plan_status;

ALTER TABLE performance_plan
  ADD CONSTRAINT ck_performance_plan_status
  CHECK (status IN ('draft', 'active', 'closed')) NOT VALID;

ALTER TABLE performance_score
  ADD COLUMN performance_base_snapshot NUMERIC(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN confirmed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE performance_score
  ADD CONSTRAINT ck_performance_score_range
  CHECK (score >= 0 AND score <= 100 AND performance_base_snapshot >= 0) NOT VALID;

CREATE UNIQUE INDEX uk_performance_score_employee_month_active
  ON performance_score(employee_id, performance_month)
  WHERE deleted = 0;

CREATE TABLE performance_plan_indicator (
  id BIGINT PRIMARY KEY,
  performance_plan_id BIGINT NOT NULL,
  indicator_id BIGINT,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(64),
  data_source VARCHAR(32) NOT NULL,
  metric_code VARCHAR(64),
  score_weight NUMERIC(12,2) NOT NULL,
  scoring_direction VARCHAR(32) NOT NULL,
  target_value NUMERIC(18,4),
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_indicator_plan FOREIGN KEY (performance_plan_id) REFERENCES performance_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_plan_indicator_source FOREIGN KEY (indicator_id) REFERENCES performance_indicator(id) NOT VALID,
  CONSTRAINT ck_plan_indicator_source CHECK (data_source IN ('manual', 'project', 'attendance', 'salary')),
  CONSTRAINT ck_plan_indicator_direction CHECK (scoring_direction IN ('higher_better', 'lower_better')),
  CONSTRAINT ck_plan_indicator_weight CHECK (score_weight >= 0 AND score_weight <= 100),
  CONSTRAINT ck_plan_indicator_target CHECK (target_value IS NULL OR target_value >= 0)
);

CREATE INDEX idx_plan_indicator_plan
  ON performance_plan_indicator(performance_plan_id, sort_order, id);

CREATE TABLE performance_score_item (
  id BIGINT PRIMARY KEY,
  performance_score_id BIGINT NOT NULL,
  plan_indicator_id BIGINT NOT NULL,
  actual_value NUMERIC(18,4),
  auto_score NUMERIC(12,2),
  final_score NUMERIC(12,2) NOT NULL DEFAULT 0,
  override_reason VARCHAR(255),
  evidence_snapshot TEXT,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_score_item_score FOREIGN KEY (performance_score_id) REFERENCES performance_score(id) ON DELETE CASCADE,
  CONSTRAINT fk_score_item_indicator FOREIGN KEY (plan_indicator_id) REFERENCES performance_plan_indicator(id),
  CONSTRAINT ck_score_item_scores CHECK (
    (auto_score IS NULL OR (auto_score >= 0 AND auto_score <= 100))
    AND final_score >= 0
    AND final_score <= 100
  ),
  CONSTRAINT uk_score_item_indicator UNIQUE (performance_score_id, plan_indicator_id)
);

CREATE INDEX idx_score_item_score ON performance_score_item(performance_score_id);

DROP INDEX IF EXISTS uk_project_member_user_active;
DROP INDEX IF EXISTS idx_project_member_user;
ALTER TABLE project_member
  DROP CONSTRAINT IF EXISTS fk_project_member_user,
  DROP CONSTRAINT IF EXISTS ck_project_member_principal;

DELETE FROM project_member WHERE employee_id IS NULL;

ALTER TABLE project_member
  ALTER COLUMN employee_id SET NOT NULL,
  DROP COLUMN user_id;

ALTER TABLE daily_work_report
  ADD COLUMN pricing_status VARCHAR(32) NOT NULL DEFAULT 'unpriced',
  ADD COLUMN pricing_source_type VARCHAR(32),
  ADD COLUMN pricing_source_id BIGINT,
  ADD COLUMN unit_price_snapshot NUMERIC(12,2) NOT NULL DEFAULT 0;

UPDATE daily_work_report
SET pricing_status = CASE
      WHEN pricing_basis IS NOT NULL
       AND pricing_basis NOT LIKE '未配置%'
      THEN 'priced'
      ELSE 'unpriced'
    END,
    pricing_source_type = CASE
      WHEN contract_item_id IS NOT NULL THEN 'contract_item'
      WHEN pricing_basis LIKE '项目工序产值单价ID:%' THEN 'project_process_price'
      ELSE NULL
    END,
    pricing_source_id = CASE
      WHEN contract_item_id IS NOT NULL THEN contract_item_id
      ELSE NULL
    END,
    unit_price_snapshot = CASE
      WHEN completed_quantity > 0 THEN ROUND(output_value / completed_quantity, 2)
      ELSE 0
    END;

ALTER TABLE daily_work_report
  ADD CONSTRAINT ck_daily_report_pricing_status
  CHECK (pricing_status IN ('priced', 'unpriced')) NOT VALID,
  ADD CONSTRAINT ck_daily_report_pricing_source
  CHECK (pricing_source_type IS NULL OR pricing_source_type IN ('contract_item', 'project_process_price')) NOT VALID,
  ADD CONSTRAINT ck_daily_report_unit_price_snapshot
  CHECK (unit_price_snapshot >= 0) NOT VALID;

CREATE INDEX idx_daily_report_pricing_status
  ON daily_work_report(pricing_status, status);
