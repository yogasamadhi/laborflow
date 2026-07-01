-- Referential integrity. NOT VALID keeps existing legacy rows from blocking the
-- deployment while still enforcing the constraints for new and updated rows.
ALTER TABLE sys_user
  ADD CONSTRAINT fk_sys_user_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT ck_sys_user_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_sys_user_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_sys_user_failed_login_count CHECK (failed_login_count >= 0) NOT VALID;

ALTER TABLE sys_role
  ADD CONSTRAINT ck_sys_role_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_sys_role_deleted CHECK (deleted IN (0, 1)) NOT VALID;

ALTER TABLE sys_user_role
  ADD CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) NOT VALID,
  ADD CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_role_user_role ON sys_user_role(user_id, role_id);

ALTER TABLE department
  ADD CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department(id) NOT VALID,
  ADD CONSTRAINT ck_department_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_department_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_department_sort_order CHECK (sort_order >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_department_parent_name_active
  ON department(COALESCE(parent_id, 0), name)
  WHERE deleted = 0;

ALTER TABLE position
  ADD CONSTRAINT fk_position_department FOREIGN KEY (department_id) REFERENCES department(id) NOT VALID,
  ADD CONSTRAINT ck_position_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_position_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_position_sort_order CHECK (sort_order >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_position_department_name_active
  ON position(COALESCE(department_id, 0), name)
  WHERE deleted = 0;

ALTER TABLE employee
  ADD CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id) NOT VALID,
  ADD CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES position(id) NOT VALID,
  ADD CONSTRAINT ck_employee_gender CHECK (gender IS NULL OR gender IN ('male', 'female')) NOT VALID,
  ADD CONSTRAINT ck_employee_type CHECK (employee_type IN ('company_employee', 'project_staff', 'temporary_worker', 'team_leader')) NOT VALID,
  ADD CONSTRAINT ck_employee_status CHECK (status IN ('active', 'inactive', 'left')) NOT VALID,
  ADD CONSTRAINT ck_employee_deleted CHECK (deleted IN (0, 1)) NOT VALID;

ALTER TABLE salary_item
  ADD CONSTRAINT ck_salary_item_type CHECK (type IN ('earning', 'deduction')) NOT VALID,
  ADD CONSTRAINT ck_salary_item_fixed CHECK (is_fixed IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_salary_item_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_salary_item_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_salary_item_sort_order CHECK (sort_order >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_salary_item_name_type_active
  ON salary_item(name, type)
  WHERE deleted = 0;

ALTER TABLE salary_profile
  ADD CONSTRAINT fk_salary_profile_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT ck_salary_profile_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_salary_profile_deleted CHECK (deleted IN (0, 1)) NOT VALID;

ALTER TABLE salary_profile_item
  ADD CONSTRAINT fk_salary_profile_item_profile FOREIGN KEY (salary_profile_id) REFERENCES salary_profile(id) NOT VALID,
  ADD CONSTRAINT fk_salary_profile_item_item FOREIGN KEY (salary_item_id) REFERENCES salary_item(id) NOT VALID,
  ADD CONSTRAINT ck_salary_profile_item_type CHECK (salary_item_type IN ('earning', 'deduction')) NOT VALID,
  ADD CONSTRAINT ck_salary_profile_item_amount CHECK (amount >= 0) NOT VALID;

ALTER TABLE salary_monthly
  ADD CONSTRAINT ck_salary_monthly_month CHECK (salary_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_status CHECK (status IN ('draft', 'pending_approval', 'approved', 'paid')) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_employee_count CHECK (total_employee_count >= 0) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_amounts CHECK (total_payable_amount >= 0 AND total_deduction_amount >= 0 AND total_actual_amount >= 0) NOT VALID;

ALTER TABLE salary_monthly_detail
  ADD CONSTRAINT fk_salary_monthly_detail_monthly FOREIGN KEY (salary_monthly_id) REFERENCES salary_monthly(id) NOT VALID,
  ADD CONSTRAINT fk_salary_monthly_detail_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT fk_salary_monthly_detail_department FOREIGN KEY (department_id) REFERENCES department(id) NOT VALID,
  ADD CONSTRAINT fk_salary_monthly_detail_position FOREIGN KEY (position_id) REFERENCES position(id) NOT VALID,
  ADD CONSTRAINT fk_salary_monthly_detail_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_detail_payment CHECK (payment_status IN ('unpaid', 'paid')) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_detail_method CHECK (payment_method IS NULL OR payment_method IN ('bank_transfer', 'cash', 'wechat', 'alipay', 'other')) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_detail_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_salary_monthly_detail_amounts CHECK (payable_amount >= 0 AND deduction_amount >= 0 AND actual_amount >= 0) NOT VALID;

ALTER TABLE salary_detail_item
  ADD CONSTRAINT fk_salary_detail_item_detail FOREIGN KEY (salary_detail_id) REFERENCES salary_monthly_detail(id) NOT VALID,
  ADD CONSTRAINT fk_salary_detail_item_item FOREIGN KEY (salary_item_id) REFERENCES salary_item(id) NOT VALID,
  ADD CONSTRAINT ck_salary_detail_item_type CHECK (salary_item_type IN ('earning', 'deduction')) NOT VALID,
  ADD CONSTRAINT ck_salary_detail_item_amount CHECK (amount >= 0) NOT VALID;

ALTER TABLE file_record
  ADD CONSTRAINT ck_file_record_size CHECK (file_size >= 0) NOT VALID;

ALTER TABLE salary_audit_record
  ADD CONSTRAINT fk_salary_audit_monthly FOREIGN KEY (salary_monthly_id) REFERENCES salary_monthly(id) NOT VALID,
  ADD CONSTRAINT fk_salary_audit_detail FOREIGN KEY (salary_detail_id) REFERENCES salary_monthly_detail(id) NOT VALID;

ALTER TABLE salary_source_record
  ADD CONSTRAINT fk_salary_source_monthly FOREIGN KEY (salary_monthly_id) REFERENCES salary_monthly(id) NOT VALID,
  ADD CONSTRAINT fk_salary_source_detail FOREIGN KEY (salary_detail_id) REFERENCES salary_monthly_detail(id) NOT VALID,
  ADD CONSTRAINT fk_salary_source_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID;

ALTER TABLE project
  ADD CONSTRAINT ck_project_status CHECK (status IN ('not_started', 'in_progress', 'paused', 'completed', 'settled')) NOT VALID,
  ADD CONSTRAINT ck_project_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_contract_amount CHECK (contract_amount >= 0) NOT VALID;

ALTER TABLE project_section
  ADD CONSTRAINT fk_project_section_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT ck_project_section_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_section_length CHECK (length >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_section_project_name_active
  ON project_section(project_id, name)
  WHERE deleted = 0;

ALTER TABLE work_process
  ADD CONSTRAINT ck_work_process_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_work_process_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_work_process_sort_order CHECK (sort_order >= 0) NOT VALID;

ALTER TABLE team
  ADD CONSTRAINT ck_team_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_team_deleted CHECK (deleted IN (0, 1)) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_name_active
  ON team(name)
  WHERE deleted = 0;

ALTER TABLE team_unit_price
  ADD CONSTRAINT fk_team_unit_price_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_team_unit_price_team FOREIGN KEY (team_id) REFERENCES team(id) NOT VALID,
  ADD CONSTRAINT fk_team_unit_price_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  ADD CONSTRAINT ck_team_unit_price_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_team_unit_price_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_team_unit_price_amount CHECK (unit_price >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_unit_price_effective_active
  ON team_unit_price(project_id, team_id, work_process_id, COALESCE(effective_date, DATE '1970-01-01'))
  WHERE deleted = 0;

ALTER TABLE project_process_price
  ADD CONSTRAINT fk_project_process_price_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_project_process_price_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  ADD CONSTRAINT ck_project_process_price_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_project_process_price_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_process_price_amount CHECK (unit_price >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_process_price_effective_active
  ON project_process_price(project_id, work_process_id, COALESCE(effective_date, DATE '1970-01-01'))
  WHERE deleted = 0;

ALTER TABLE daily_work_report
  ADD CONSTRAINT fk_daily_work_report_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_daily_work_report_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  ADD CONSTRAINT fk_daily_work_report_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  ADD CONSTRAINT fk_daily_work_report_team FOREIGN KEY (team_id) REFERENCES team(id) NOT VALID,
  ADD CONSTRAINT ck_daily_work_report_status CHECK (status IN ('draft', 'submitted', 'pm_confirmed', 'confirmed', 'rejected')) NOT VALID,
  ADD CONSTRAINT ck_daily_work_report_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_daily_work_report_numbers CHECK (completed_quantity >= 0 AND worker_count >= 0) NOT VALID;

ALTER TABLE daily_work_report_file
  ADD CONSTRAINT fk_daily_work_report_file_report FOREIGN KEY (daily_work_report_id) REFERENCES daily_work_report(id) NOT VALID,
  ADD CONSTRAINT fk_daily_work_report_file_file FOREIGN KEY (file_id) REFERENCES file_record(id) NOT VALID,
  ADD CONSTRAINT ck_daily_work_report_file_type CHECK (file_type IN ('photo', 'evidence', 'voucher', 'document', 'other')) NOT VALID;

ALTER TABLE work_quantity_confirm
  ADD CONSTRAINT fk_work_quantity_confirm_report FOREIGN KEY (daily_work_report_id) REFERENCES daily_work_report(id) NOT VALID;

ALTER TABLE team_settlement
  ADD CONSTRAINT fk_team_settlement_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_team_settlement_team FOREIGN KEY (team_id) REFERENCES team(id) NOT VALID,
  ADD CONSTRAINT ck_team_settlement_month CHECK (settlement_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_team_settlement_status CHECK (settlement_status IN ('draft', 'confirmed')) NOT VALID,
  ADD CONSTRAINT ck_team_settlement_payment CHECK (payment_status IN ('unpaid', 'paid')) NOT VALID,
  ADD CONSTRAINT ck_team_settlement_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_team_settlement_amounts CHECK (confirmed_quantity >= 0 AND settlement_amount >= 0 AND compensation_amount >= 0 AND deduction_amount >= 0 AND actual_pay_amount >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_settlement_month_active
  ON team_settlement(project_id, team_id, settlement_month)
  WHERE deleted = 0;

ALTER TABLE team_settlement_detail
  ADD CONSTRAINT fk_team_settlement_detail_settlement FOREIGN KEY (team_settlement_id) REFERENCES team_settlement(id) NOT VALID,
  ADD CONSTRAINT fk_team_settlement_detail_report FOREIGN KEY (daily_work_report_id) REFERENCES daily_work_report(id) NOT VALID,
  ADD CONSTRAINT fk_team_settlement_detail_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  ADD CONSTRAINT ck_team_settlement_detail_amounts CHECK (quantity >= 0 AND unit_price >= 0 AND amount >= 0) NOT VALID;

ALTER TABLE variation_order
  ADD CONSTRAINT fk_variation_order_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_variation_order_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  ADD CONSTRAINT ck_variation_order_status CHECK (status IN ('draft', 'submitted', 'confirmed', 'rejected')) NOT VALID,
  ADD CONSTRAINT ck_variation_order_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_variation_order_numbers CHECK (increased_quantity >= 0 AND estimated_amount >= 0 AND labor_count >= 0) NOT VALID;

ALTER TABLE variation_order_file
  ADD CONSTRAINT fk_variation_order_file_order FOREIGN KEY (variation_order_id) REFERENCES variation_order(id) NOT VALID,
  ADD CONSTRAINT fk_variation_order_file_file FOREIGN KEY (file_id) REFERENCES file_record(id) NOT VALID,
  ADD CONSTRAINT ck_variation_order_file_type CHECK (file_type IN ('photo', 'evidence', 'voucher', 'document', 'other')) NOT VALID;

ALTER TABLE variation_order_confirm
  ADD CONSTRAINT fk_variation_order_confirm_order FOREIGN KEY (variation_order_id) REFERENCES variation_order(id) NOT VALID;

ALTER TABLE project_cost_record
  ADD CONSTRAINT fk_project_cost_record_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT ck_project_cost_record_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_cost_record_amount CHECK (amount >= 0) NOT VALID;

ALTER TABLE project_payment_record
  ADD CONSTRAINT fk_project_payment_record_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_project_payment_record_file FOREIGN KEY (voucher_file_id) REFERENCES file_record(id) NOT VALID,
  ADD CONSTRAINT ck_project_payment_record_type CHECK (record_type IN ('receivable', 'received')) NOT VALID,
  ADD CONSTRAINT ck_project_payment_record_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_payment_record_amount CHECK (amount >= 0) NOT VALID;

ALTER TABLE machinery_equipment
  ADD CONSTRAINT ck_machinery_equipment_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_machinery_equipment_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_machinery_equipment_price CHECK (unit_price >= 0) NOT VALID;

ALTER TABLE machinery_work_record
  ADD CONSTRAINT fk_machinery_work_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_machinery_work_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  ADD CONSTRAINT fk_machinery_work_equipment FOREIGN KEY (machinery_equipment_id) REFERENCES machinery_equipment(id) NOT VALID,
  ADD CONSTRAINT ck_machinery_work_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_machinery_work_amounts CHECK (shift_count >= 0 AND unit_price >= 0 AND amount >= 0) NOT VALID;

ALTER TABLE material_record
  ADD CONSTRAINT fk_material_record_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_material_record_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  ADD CONSTRAINT ck_material_record_type CHECK (record_type IN ('arrival', 'usage', 'impact')) NOT VALID,
  ADD CONSTRAINT ck_material_record_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_material_record_amounts CHECK (quantity >= 0 AND unit_price >= 0 AND amount >= 0) NOT VALID;

ALTER TABLE project_document_archive
  ADD CONSTRAINT fk_project_document_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_project_document_section FOREIGN KEY (project_section_id) REFERENCES project_section(id) NOT VALID,
  ADD CONSTRAINT fk_project_document_process FOREIGN KEY (work_process_id) REFERENCES work_process(id) NOT VALID,
  ADD CONSTRAINT fk_project_document_file FOREIGN KEY (file_id) REFERENCES file_record(id) NOT VALID,
  ADD CONSTRAINT ck_project_document_deleted CHECK (deleted IN (0, 1)) NOT VALID;

ALTER TABLE attendance_record
  ADD CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT fk_attendance_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT ck_attendance_month CHECK (attendance_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_attendance_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_attendance_numbers CHECK (work_days >= 0 AND leave_days >= 0 AND overtime_days >= 0 AND absent_days >= 0 AND field_days >= 0 AND project_site_days >= 0 AND deduction_amount >= 0) NOT VALID;

ALTER TABLE performance_indicator
  ADD CONSTRAINT fk_performance_indicator_position FOREIGN KEY (position_id) REFERENCES position(id) NOT VALID,
  ADD CONSTRAINT ck_performance_indicator_data_source CHECK (data_source IN ('manual', 'project', 'attendance', 'salary')) NOT VALID,
  ADD CONSTRAINT ck_performance_indicator_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_performance_indicator_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_performance_indicator_weight CHECK (score_weight >= 0) NOT VALID;

ALTER TABLE performance_plan
  ADD CONSTRAINT fk_performance_plan_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_performance_plan_position FOREIGN KEY (position_id) REFERENCES position(id) NOT VALID,
  ADD CONSTRAINT ck_performance_plan_month CHECK (performance_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_performance_plan_status CHECK (status IN ('draft', 'active', 'inactive', 'confirmed')) NOT VALID,
  ADD CONSTRAINT ck_performance_plan_deleted CHECK (deleted IN (0, 1)) NOT VALID;

ALTER TABLE performance_score
  ADD CONSTRAINT fk_performance_score_plan FOREIGN KEY (performance_plan_id) REFERENCES performance_plan(id) NOT VALID,
  ADD CONSTRAINT fk_performance_score_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_performance_score_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT fk_performance_score_position FOREIGN KEY (position_id) REFERENCES position(id) NOT VALID,
  ADD CONSTRAINT ck_performance_score_month CHECK (performance_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_performance_score_status CHECK (status IN ('draft', 'confirmed')) NOT VALID,
  ADD CONSTRAINT ck_performance_score_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_performance_score_amounts CHECK (score >= 0 AND performance_amount >= 0) NOT VALID;

ALTER TABLE project_bonus_pool
  ADD CONSTRAINT fk_project_bonus_pool_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_pool_month CHECK (bonus_month IS NULL OR bonus_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_project_bonus_pool_status CHECK (release_status IN ('draft', 'released')) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_pool_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_pool_amounts CHECK (final_gross_profit >= 0 AND bonus_ratio >= 0 AND bonus_pool_amount >= 0) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_bonus_pool_month_active
  ON project_bonus_pool(project_id, bonus_month)
  WHERE deleted = 0 AND bonus_month IS NOT NULL;

ALTER TABLE project_bonus_allocation
  ADD CONSTRAINT fk_project_bonus_allocation_pool FOREIGN KEY (bonus_pool_id) REFERENCES project_bonus_pool(id) NOT VALID,
  ADD CONSTRAINT fk_project_bonus_allocation_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_project_bonus_allocation_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_allocation_month CHECK (payment_month ~ '^\d{4}-(0[1-9]|1[0-2])$') NOT VALID,
  ADD CONSTRAINT ck_project_bonus_allocation_status CHECK (status IN ('allocated', 'confirmed')) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_allocation_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_bonus_allocation_amounts CHECK (allocation_ratio >= 0 AND allocation_amount >= 0) NOT VALID;

ALTER TABLE project_member
  ADD CONSTRAINT fk_project_member_project FOREIGN KEY (project_id) REFERENCES project(id) NOT VALID,
  ADD CONSTRAINT fk_project_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) NOT VALID,
  ADD CONSTRAINT fk_project_member_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT ck_project_member_status CHECK (status IN ('active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_project_member_deleted CHECK (deleted IN (0, 1)) NOT VALID,
  ADD CONSTRAINT ck_project_member_principal CHECK (user_id IS NOT NULL OR employee_id IS NOT NULL) NOT VALID;

ALTER TABLE wechat_user_binding
  ADD CONSTRAINT fk_wechat_user_binding_user FOREIGN KEY (user_id) REFERENCES sys_user(id) NOT VALID,
  ADD CONSTRAINT fk_wechat_user_binding_employee FOREIGN KEY (employee_id) REFERENCES employee(id) NOT VALID,
  ADD CONSTRAINT ck_wechat_user_binding_status CHECK (status IN ('pending', 'active', 'inactive')) NOT VALID,
  ADD CONSTRAINT ck_wechat_user_binding_deleted CHECK (deleted IN (0, 1)) NOT VALID;
