-- BẢN ĐỀ XUẤT TỔNG HỢP: chờ Toàn, Tú, Tín, Nguyên rà soát.
-- MySQL >= 8.0.16 / InnoDB / utf8mb4. Chạy trên CSDL thử rỗng.
-- DATETIME(3) lưu UTC do ứng dụng/JDBC bảo đảm, không có DEFAULT CURRENT_TIMESTAMP.
-- Không tạo database, không xóa dữ liệu. Không áp dụng production khi chưa review.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE roles (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(40) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL,
 description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE permissions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(60) NOT NULL UNIQUE,
 description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE role_permissions (
 role_id BIGINT UNSIGNED NOT NULL,
 permission_id BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY (role_id, permission_id),
 FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
 FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE accounts (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(100) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 full_name VARCHAR(200) NOT NULL,
 role_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(3) NOT NULL,
 updated_at DATETIME(3) NOT NULL,
 CHECK (status IN ('ACTIVE','LOCKED')),
 FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE login_sessions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 account_id BIGINT UNSIGNED NOT NULL,
 token_hash VARCHAR(255) NOT NULL UNIQUE,
 created_at DATETIME(3) NOT NULL,
 expires_at DATETIME(3) NOT NULL,
 revoked_at DATETIME(3) NULL,
 INDEX idx_sessions_account (account_id),
 CHECK (expires_at > created_at),
 FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE employees (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 account_id BIGINT UNSIGNED NOT NULL UNIQUE,
 employee_code VARCHAR(30) NOT NULL UNIQUE,
 status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
 CHECK (status IN ('ACTIVE','INACTIVE')),
 FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE registers (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(30) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
 CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE shifts (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 shift_no VARCHAR(30) NOT NULL UNIQUE,
 employee_id BIGINT UNSIGNED NOT NULL,
 register_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
 open_request_key VARCHAR(64) NOT NULL UNIQUE,
 opened_at DATETIME(3) NOT NULL,
 closed_at DATETIME(3) NULL,
 opening_cash DECIMAL(18,2) NOT NULL,
 closing_revenue DECIMAL(18,2) NULL,
 closing_cash_sales DECIMAL(18,2) NULL,
 closing_expected_cash DECIMAL(18,2) NULL,
 closing_actual_cash DECIMAL(18,2) NULL,
 closing_difference DECIMAL(18,2) NULL,
 open_employee_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='OPEN' THEN employee_id ELSE NULL END) STORED,
 UNIQUE KEY uq_open_employee (open_employee_id),
 CHECK (opening_cash >= 0),
 CHECK ((status='OPEN' AND closed_at IS NULL AND closing_revenue IS NULL AND closing_cash_sales IS NULL AND closing_expected_cash IS NULL AND closing_actual_cash IS NULL AND closing_difference IS NULL) OR (status='CLOSED' AND closed_at IS NOT NULL AND closed_at >= opened_at AND closing_revenue IS NOT NULL AND closing_revenue >= 0 AND closing_cash_sales IS NOT NULL AND closing_cash_sales >= 0 AND closing_expected_cash IS NOT NULL AND closing_actual_cash IS NOT NULL AND closing_expected_cash >= 0 AND closing_actual_cash >= 0 AND closing_difference IS NOT NULL AND closing_difference = closing_actual_cash - closing_expected_cash AND closing_expected_cash = opening_cash + closing_cash_sales)),
 FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
 FOREIGN KEY (register_id) REFERENCES registers(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE product_categories (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(20) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
 CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE units (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(12) NOT NULL UNIQUE,
 name VARCHAR(50) NOT NULL,
 quantity_scale TINYINT UNSIGNED NOT NULL DEFAULT 0,
 CHECK (quantity_scale IN (0,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE products (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(20) NOT NULL UNIQUE,
 barcode VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 category_id BIGINT UNSIGNED NOT NULL,
 unit_id BIGINT UNSIGNED NOT NULL,
 sale_price DECIMAL(18,2) NOT NULL,
 stock_quantity DECIMAL(15,3) NOT NULL DEFAULT 0,
 reserved_quantity DECIMAL(15,3) NOT NULL DEFAULT 0,
 image_url VARCHAR(500) NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
 stopped_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL,
 updated_at DATETIME(3) NOT NULL,
 CHECK (sale_price > 0 AND stock_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= stock_quantity),
 CHECK ((status='ACTIVE' AND stopped_at IS NULL) OR (status='STOPPED' AND stopped_at IS NOT NULL)),
 FOREIGN KEY (category_id) REFERENCES product_categories(id) ON DELETE RESTRICT,
 FOREIGN KEY (unit_id) REFERENCES units(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE promotions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(20) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 discount_percent DECIMAL(5,2) NOT NULL,
 starts_at DATETIME(3) NOT NULL,
 ends_at DATETIME(3) NOT NULL,
 lifecycle_status VARCHAR(10) NOT NULL DEFAULT 'ENABLED',
 canceled_at DATETIME(3) NULL,
 CHECK (discount_percent > 0 AND discount_percent <= 100 AND starts_at < ends_at),
 CHECK ((lifecycle_status='ENABLED' AND canceled_at IS NULL) OR (lifecycle_status='CANCELED' AND canceled_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE promotion_products (
 promotion_id BIGINT UNSIGNED NOT NULL,
 product_id BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY (promotion_id,product_id),
 FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE RESTRICT,
 FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE orders (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 order_no VARCHAR(30) NOT NULL UNIQUE,
 shift_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
 subtotal_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL,
 paid_at DATETIME(3) NULL,
 cancelled_at DATETIME(3) NULL,
 cancellation_reason VARCHAR(255) NULL,
 create_request_key VARCHAR(64) NOT NULL UNIQUE,
 CHECK (status IN ('DRAFT','PENDING','PAID','CANCELLED')),
 CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND discount_amount <= subtotal_amount AND total_amount = subtotal_amount - discount_amount),
 CHECK ((status='PAID' AND paid_at IS NOT NULL) OR (status <> 'PAID' AND paid_at IS NULL)),
 CHECK ((status='CANCELLED' AND cancelled_at IS NOT NULL) OR (status <> 'CANCELLED' AND cancelled_at IS NULL)),
 INDEX idx_orders_shift_status (shift_id,status),
 FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE order_items (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT UNSIGNED NOT NULL,
 product_id BIGINT UNSIGNED NOT NULL,
 product_code_snapshot VARCHAR(20) NOT NULL,
 product_name_snapshot VARCHAR(200) NOT NULL,
 barcode_snapshot VARCHAR(50) NOT NULL,
 unit_code_snapshot VARCHAR(12) NOT NULL,
 quantity DECIMAL(15,3) NOT NULL,
 unit_price DECIMAL(18,2) NOT NULL,
 promotion_id BIGINT UNSIGNED NULL,
 promotion_code_snapshot VARCHAR(20) NULL,
 promotion_name_snapshot VARCHAR(200) NULL,
 discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 line_total DECIMAL(18,2) NOT NULL,
 UNIQUE KEY uq_item_product (order_id,product_id),
 CHECK (quantity > 0 AND unit_price > 0 AND discount_amount >= 0 AND discount_amount <= ROUND(quantity * unit_price, 2) AND line_total = ROUND(quantity * unit_price, 2) - discount_amount),
 CHECK ((promotion_id IS NULL AND promotion_code_snapshot IS NULL AND promotion_name_snapshot IS NULL AND discount_amount=0) OR (promotion_id IS NOT NULL AND promotion_code_snapshot IS NOT NULL AND promotion_name_snapshot IS NOT NULL)),
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
 FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
 FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE payments (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT UNSIGNED NOT NULL,
 method VARCHAR(16) NOT NULL DEFAULT 'CASH',
 status VARCHAR(10) NOT NULL,
 amount_due DECIMAL(18,2) NOT NULL,
 amount_received DECIMAL(18,2) NOT NULL DEFAULT 0,
 change_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 idempotency_key VARCHAR(64) NOT NULL UNIQUE,
 bank_reference VARCHAR(100) NULL UNIQUE,
 confirmed_by BIGINT UNSIGNED NULL,
 confirmed_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL,
 completed_at DATETIME(3) NULL,
 success_order_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='SUCCESS' THEN order_id ELSE NULL END) STORED,
 pending_order_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN order_id ELSE NULL END) STORED,
 UNIQUE KEY uq_payment_order (id,order_id),
 UNIQUE KEY uq_payment_success (success_order_id),
 UNIQUE KEY uq_payment_pending (pending_order_id),
 CONSTRAINT ck_payment_method CHECK (method IN ('CASH','BANK_TRANSFER')),
 CONSTRAINT ck_transfer_confirmation CHECK (
   (status='SUCCESS' AND confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL AND
     ((method='CASH' AND bank_reference IS NULL) OR
      (method='BANK_TRANSFER' AND bank_reference IS NOT NULL AND TRIM(bank_reference)<>'' AND amount_received=amount_due AND change_amount=0)))
   OR (status<>'SUCCESS' AND bank_reference IS NULL AND confirmed_by IS NULL AND confirmed_at IS NULL)),
 CHECK (status IN ('PENDING','SUCCESS','FAILED')),
 CHECK (amount_due >= 0 AND amount_received >= 0 AND change_amount >= 0),
 CHECK ((status='SUCCESS' AND completed_at IS NOT NULL AND amount_received >= amount_due AND change_amount=amount_received-amount_due) OR (status='FAILED' AND completed_at IS NOT NULL AND amount_received=0 AND change_amount=0) OR (status='PENDING' AND completed_at IS NULL AND amount_received=0 AND change_amount=0)),
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
 FOREIGN KEY (confirmed_by) REFERENCES accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE invoices (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 invoice_no VARCHAR(30) NOT NULL UNIQUE,
 order_id BIGINT UNSIGNED NOT NULL UNIQUE,
 payment_id BIGINT UNSIGNED NOT NULL UNIQUE,
 issued_by BIGINT UNSIGNED NOT NULL,
 cashier_name_snapshot VARCHAR(200) NOT NULL,
 issued_at DATETIME(3) NOT NULL,
 subtotal_amount DECIMAL(18,2) NOT NULL,
 discount_amount DECIMAL(18,2) NOT NULL,
 total_amount DECIMAL(18,2) NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'ISSUED',
 CHECK (status IN ('ISSUED','VOID')),
 CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND discount_amount <= subtotal_amount AND total_amount=subtotal_amount-discount_amount),
 INDEX idx_invoice_status_date (status,issued_at),
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
 FOREIGN KEY (payment_id,order_id) REFERENCES payments(id,order_id) ON DELETE RESTRICT,
 FOREIGN KEY (issued_by) REFERENCES accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE inventory_movements (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 event_key VARCHAR(64) NOT NULL UNIQUE,
 product_id BIGINT UNSIGNED NULL,
 product_code_snapshot VARCHAR(20) NOT NULL,
 order_item_id BIGINT UNSIGNED NULL,
 movement_type VARCHAR(10) NOT NULL,
 quantity_delta DECIMAL(15,3) NOT NULL,
 reason VARCHAR(255) NOT NULL,
 created_at DATETIME(3) NOT NULL,
 CHECK ((movement_type='SALE' AND quantity_delta < 0 AND order_item_id IS NOT NULL) OR (movement_type='ADJUSTMENT' AND quantity_delta <> 0)),
 FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
 FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


