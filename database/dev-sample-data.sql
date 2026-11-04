-- Dữ liệu mẫu dùng cho môi trường phát triển (DEV)
-- Chạy file này SAU KHI đã chạy schema.sql và seed.sql trên database pos_supermarket.
-- Mật khẩu mặc định cho tất cả tài khoản dưới đây là: demo123

SET NAMES utf8mb4;

-- 1. Tài khoản thử nghiệm (Mật khẩu: demo123)
-- role_id = 1: CASHIER, role_id = 2: MANAGER, role_id = 3: ADMIN
INSERT INTO accounts(id, username, password_hash, full_name, role_id, status, created_at, updated_at) VALUES
(1, 'cashier', '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Trần Minh Anh (Thu ngân)', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'manager', '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Nguyễn Lan (Quản lý)', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'admin',   '$2a$10$QbMKd6d7ZgrRd8SuRI.5kei.a9WdRv.vF/Jj0JCUpepDlSx8SgHEK', 'Vũ Hoài Nam (Quản trị)', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 2. Gán nhân viên cho thu ngân (để mở ca)
INSERT INTO employees(id, account_id, employee_code) VALUES
(1, 1, 'NV01')
ON DUPLICATE KEY UPDATE employee_code=VALUES(employee_code);

-- 3. Sản phẩm mẫu siêu thị
INSERT INTO products(id, code, barcode, name, category_id, unit_id, sale_price, stock_quantity, created_at, updated_at) VALUES
(1, 'SP001', '8934673120012', 'Sữa tươi Vinamilk 1L', 1, 1, 32000, 86, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'SP002', '8934588010011', 'Bánh quy bơ Cosy 200g', 2, 1, 20000, 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'SP003', '8934803040011', 'Nước suối Aquafina 500ml', 3, 2, 6000, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'SP004', '8934563130012', 'Mì Hảo Hảo tôm chua cay', 2, 3, 4500, 156, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'SP005', '8936017360012', 'Dầu ăn Simply 1L', 2, 2, 52000, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name=VALUES(name), sale_price=VALUES(sale_price), stock_quantity=VALUES(stock_quantity);
