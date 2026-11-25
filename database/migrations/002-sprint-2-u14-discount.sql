-- U1.4: Coupons and order/item discount support
CREATE TABLE IF NOT EXISTS coupons (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(30) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 discount_type VARCHAR(10) NOT NULL,
 discount_value DECIMAL(18,2) NOT NULL,
 min_order_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 starts_at DATETIME(3) NOT NULL,
 ends_at DATETIME(3) NOT NULL,
 lifecycle_status VARCHAR(10) NOT NULL DEFAULT 'ENABLED',
 CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
 CHECK ((discount_type='PERCENT' AND discount_value > 0 AND discount_value <= 100) OR (discount_type='AMOUNT' AND discount_value > 0)),
 CHECK (starts_at < ends_at),
 CHECK (lifecycle_status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE orders
  ADD COLUMN discount_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
  ADD COLUMN coupon_id BIGINT UNSIGNED NULL,
  ADD COLUMN coupon_code_snapshot VARCHAR(30) NULL,
  ADD COLUMN approved_by BIGINT UNSIGNED NULL,
  ADD CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_order_approver FOREIGN KEY (approved_by) REFERENCES accounts(id) ON DELETE RESTRICT;

-- Allow order_items.discount_amount > 0 for coupon / direct discounts without promotion_id
SET @old_promo_check = (
  SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc
  JOIN information_schema.CHECK_CONSTRAINTS cc
    ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
  WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='order_items' AND tc.CONSTRAINT_TYPE='CHECK'
    AND LOWER(cc.CHECK_CLAUSE) LIKE '%promotion_id%' LIMIT 1
);
SET @drop_promo_check = IF(@old_promo_check IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE order_items DROP CHECK `',REPLACE(@old_promo_check,'`','``'),'`'));
PREPARE promo_check_stmt FROM @drop_promo_check;
EXECUTE promo_check_stmt;
DEALLOCATE PREPARE promo_check_stmt;
