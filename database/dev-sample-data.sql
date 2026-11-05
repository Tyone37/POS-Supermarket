-- Dữ liệu mẫu hoàn chỉnh dùng cho môi trường phát triển (DEV)
-- Tự động đảm bảo có đầy đủ Roles, Lookup data trước khi tạo Accounts và Products
-- Mật khẩu mặc định cho tất cả tài khoản mẫu: demo123

SET NAMES utf8mb4;

-- 1. Bổ sung roles (nếu chưa có)
INSERT INTO roles(code, name) VALUES
 ('CASHIER', 'Thu ngân'),
 ('MANAGER', 'Quản lý'),
 ('ADMIN', 'Quản trị tài khoản')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Bổ sung permissions (nếu chưa có)
INSERT INTO permissions(code, description) VALUES
 ('SALE_CREATE', 'Bán hàng tại quầy'),
 ('SHIFT_OPEN', 'Mở ca của mình'),
 ('INVOICE_READ', 'Xem hóa đơn theo phạm vi vai trò'),
 ('PRODUCT_MANAGE', 'Quản lý sản phẩm'),
 ('ACCOUNT_MANAGE', 'Tạo và khóa tài khoản')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 3. Gán quyền cho roles
INSERT INTO role_permissions(role_id, permission_id)
 SELECT r.id, p.id FROM roles r JOIN permissions p ON
 (r.code='CASHIER' AND p.code IN ('SALE_CREATE','SHIFT_OPEN','INVOICE_READ')) OR
 (r.code='MANAGER' AND p.code IN ('PRODUCT_MANAGE','INVOICE_READ')) OR
 (r.code='ADMIN' AND p.code='ACCOUNT_MANAGE')
ON DUPLICATE KEY UPDATE permission_id=VALUES(permission_id);

-- 4. Bổ sung quầy bán hàng
INSERT INTO registers(code, name, status) VALUES
 ('Q01', 'Quầy 1', 'ACTIVE'),
 ('Q02', 'Quầy 2', 'ACTIVE')
ON DUPLICATE KEY UPDATE code=VALUES(code);

-- 5. Bổ sung nhóm hàng & đơn vị
INSERT INTO product_categories(code, name, status) VALUES
 ('DAIRY', 'Sữa và trứng', 'ACTIVE'),
 ('FOOD', 'Thực phẩm', 'ACTIVE'),
 ('DRINK', 'Đồ uống', 'ACTIVE')
ON DUPLICATE KEY UPDATE code=VALUES(code);

INSERT INTO units(code, name, quantity_scale) VALUES
 ('HOP', 'Hộp', 0),
 ('CHAI', 'Chai', 0),
 ('CAI', 'Cái', 0),
 ('KG', 'Kilôgam', 3)
ON DUPLICATE KEY UPDATE code=VALUES(code);

-- 6. Tạo tài khoản thử nghiệm (Mật khẩu: demo123)
-- Truy vấn role_id tự động theo code, không hardcode số ID để tránh lỗi Foreign Key
INSERT INTO accounts(username, password_hash, full_name, role_id, status, created_at, updated_at)
VALUES
 ('cashier', '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Trần Minh Anh (Thu ngân)', (SELECT id FROM roles WHERE code='CASHIER' LIMIT 1), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('manager', '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Nguyễn Lan (Quản lý)', (SELECT id FROM roles WHERE code='MANAGER' LIMIT 1), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('admin',   '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Vũ Hoài Nam (Quản trị)', (SELECT id FROM roles WHERE code='ADMIN' LIMIT 1), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 7. Gán nhân viên cho thu ngân (để mở ca)
INSERT INTO employees(account_id, employee_code)
VALUES
 ((SELECT id FROM accounts WHERE username='cashier' LIMIT 1), 'NV01')
ON DUPLICATE KEY UPDATE employee_code=VALUES(employee_code);

-- 8. Sản phẩm mẫu siêu thị
INSERT INTO products(code, barcode, name, category_id, unit_id, sale_price, stock_quantity, created_at, updated_at)
VALUES
 ('SP001', '8934673120012', 'Sữa tươi Vinamilk 1L', (SELECT id FROM product_categories WHERE code='DAIRY' LIMIT 1), (SELECT id FROM units WHERE code='HOP' LIMIT 1), 32000, 86, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('SP002', '8934588010011', 'Bánh quy bơ Cosy 200g', (SELECT id FROM product_categories WHERE code='FOOD' LIMIT 1), (SELECT id FROM units WHERE code='HOP' LIMIT 1), 20000, 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('SP003', '8934803040011', 'Nước suối Aquafina 500ml', (SELECT id FROM product_categories WHERE code='DRINK' LIMIT 1), (SELECT id FROM units WHERE code='CHAI' LIMIT 1), 6000, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('SP004', '8934563130012', 'Mì Hảo Hảo tôm chua cay', (SELECT id FROM product_categories WHERE code='FOOD' LIMIT 1), (SELECT id FROM units WHERE code='CAI' LIMIT 1), 4500, 156, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 ('SP005', '8936017360012', 'Dầu ăn Simply 1L', (SELECT id FROM product_categories WHERE code='FOOD' LIMIT 1), (SELECT id FROM units WHERE code='CHAI' LIMIT 1), 52000, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name=VALUES(name), sale_price=VALUES(sale_price), stock_quantity=VALUES(stock_quantity);
