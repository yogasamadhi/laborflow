ALTER TABLE project_payment_record
  ADD COLUMN invoice_no VARCHAR(64),
  ADD COLUMN invoice_date DATE,
  ADD COLUMN retention_due_date DATE;

ALTER TABLE project_payment_record
  DROP CONSTRAINT IF EXISTS ck_project_payment_record_type;

ALTER TABLE project_payment_record
  ADD CONSTRAINT ck_project_payment_record_type CHECK (
    record_type IN ('receivable', 'received', 'invoice', 'retention_withheld', 'retention_returned')
  ) NOT VALID;

CREATE INDEX idx_project_payment_invoice ON project_payment_record(project_id, invoice_date)
  WHERE record_type = 'invoice' AND deleted = 0;

CREATE INDEX idx_project_payment_retention ON project_payment_record(project_id, retention_due_date)
  WHERE record_type IN ('retention_withheld', 'retention_returned') AND deleted = 0;
