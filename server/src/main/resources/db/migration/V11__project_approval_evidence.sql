ALTER TABLE work_quantity_confirm
  ADD COLUMN status_before VARCHAR(32),
  ADD COLUMN status_after VARCHAR(32);

CREATE INDEX idx_work_quantity_confirm_report ON work_quantity_confirm(daily_work_report_id, created_at);
CREATE INDEX idx_daily_work_report_file_report ON daily_work_report_file(daily_work_report_id, created_at);
CREATE INDEX idx_variation_order_file_order ON variation_order_file(variation_order_id, created_at);

CREATE TABLE variation_order_confirm (
  id BIGINT PRIMARY KEY,
  variation_order_id BIGINT NOT NULL,
  confirm_stage VARCHAR(32) NOT NULL,
  confirm_status VARCHAR(32) NOT NULL,
  confirmer_id BIGINT,
  confirmer_name VARCHAR(64),
  status_before VARCHAR(32),
  status_after VARCHAR(32),
  remark VARCHAR(255),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_variation_order_confirm_order ON variation_order_confirm(variation_order_id, created_at);
