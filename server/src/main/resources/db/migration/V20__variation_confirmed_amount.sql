ALTER TABLE variation_order
  ADD COLUMN confirmed_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN confirmed_at TIMESTAMP WITHOUT TIME ZONE,
  ADD COLUMN confirmer_id BIGINT,
  ADD COLUMN confirmer_name VARCHAR(64),
  ADD COLUMN confirm_remark VARCHAR(255);

UPDATE variation_order
SET confirmed_amount = estimated_amount
WHERE status = 'confirmed'
  AND confirmed_amount = 0;

ALTER TABLE variation_order
  ADD CONSTRAINT ck_variation_order_confirmed_amount CHECK (confirmed_amount >= 0) NOT VALID;
