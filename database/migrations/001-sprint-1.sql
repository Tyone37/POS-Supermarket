-- MySQL >= 8.0.16. Run ONCE on the OLD schema, while application is stopped.
-- For a NEW empty database use schema.sql instead, not both scripts.
-- Back up existing data first. No tables/data are deleted.
SET time_zone = '+00:00';
ALTER TABLE invoices ADD COLUMN cashier_name_snapshot VARCHAR(200) NULL;
UPDATE invoices i JOIN accounts a ON a.id=i.issued_by SET i.cashier_name_snapshot=a.full_name;
ALTER TABLE invoices MODIFY cashier_name_snapshot VARCHAR(200) NOT NULL;
ALTER TABLE products ADD COLUMN image_url VARCHAR(500) NULL;
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(255) NULL,
  ADD COLUMN create_request_key VARCHAR(64) NULL;
UPDATE orders SET create_request_key=CONCAT('legacy-order-',id) WHERE create_request_key IS NULL;
ALTER TABLE orders MODIFY create_request_key VARCHAR(64) NOT NULL,
  ADD UNIQUE KEY uq_order_create_request(create_request_key);

-- The old CASH-only CHECK was anonymous. Resolve its actual MySQL-generated name.
SET @old_method_check = (
  SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc
  JOIN information_schema.CHECK_CONSTRAINTS cc
    ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
  WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='payments' AND tc.CONSTRAINT_TYPE='CHECK'
    AND LOWER(cc.CHECK_CLAUSE) LIKE '%method%' LIMIT 1
);
SET @drop_method_check = IF(@old_method_check IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE payments DROP CHECK `',REPLACE(@old_method_check,'`','``'),'`'));
PREPARE payment_check_stmt FROM @drop_method_check;
EXECUTE payment_check_stmt;
DEALLOCATE PREPARE payment_check_stmt;
ALTER TABLE payments MODIFY method VARCHAR(16) NOT NULL DEFAULT 'CASH',
  ADD COLUMN bank_reference VARCHAR(100) NULL,
  ADD COLUMN confirmed_by BIGINT UNSIGNED NULL,
  ADD COLUMN confirmed_at DATETIME(3) NULL,
  ADD UNIQUE KEY uq_bank_reference(bank_reference),
  ADD CONSTRAINT fk_payment_confirmer FOREIGN KEY (confirmed_by) REFERENCES accounts(id) ON DELETE RESTRICT,
  ADD CONSTRAINT ck_payment_method CHECK(method IN ('CASH','BANK_TRANSFER'));
-- Preserve the actor/time of old successful cash payments from their invoice.
UPDATE payments p JOIN invoices i ON i.payment_id=p.id
  SET p.confirmed_by=i.issued_by,p.confirmed_at=p.completed_at WHERE p.status='SUCCESS';
-- This CHECK intentionally refuses legacy successful payments without an invoice/actor.
-- Reconcile such records before applying this last ALTER; never invent confirmation evidence.
ALTER TABLE payments ADD CONSTRAINT ck_transfer_confirmation CHECK (
  (status='SUCCESS' AND confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL AND
    ((method='CASH' AND bank_reference IS NULL) OR
     (method='BANK_TRANSFER' AND bank_reference IS NOT NULL AND TRIM(bank_reference)<>'' AND amount_received=amount_due AND change_amount=0)))
  OR (status<>'SUCCESS' AND bank_reference IS NULL AND confirmed_by IS NULL AND confirmed_at IS NULL)
);
