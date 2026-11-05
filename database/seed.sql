-- Lookup data only; run after schema.sql (new DB) OR migration (existing DB).
-- No accounts, passwords, customer data or payment destinations are seeded.
SET NAMES utf8mb4;
INSERT INTO roles(code,name) VALUES
 ('CASHIER','Thu ngân'),('MANAGER','Quản lý'),('ADMIN','Quản trị tài khoản')
ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO permissions(code,description) VALUES
 ('SALE_CREATE','Bán hàng tại quầy'),('SHIFT_OPEN','Mở ca của mình'),
 ('INVOICE_READ','Xem hóa đơn theo phạm vi vai trò'),('PRODUCT_MANAGE','Quản lý sản phẩm'),
 ('ACCOUNT_MANAGE','Tạo và khóa tài khoản')
ON DUPLICATE KEY UPDATE description=VALUES(description);
INSERT INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r JOIN permissions p ON
 (r.code='CASHIER' AND p.code IN ('SALE_CREATE','SHIFT_OPEN','INVOICE_READ')) OR
 (r.code='MANAGER' AND p.code IN ('PRODUCT_MANAGE','INVOICE_READ')) OR
 (r.code='ADMIN' AND p.code='ACCOUNT_MANAGE')
ON DUPLICATE KEY UPDATE permission_id=VALUES(permission_id);
INSERT INTO registers(code,name,status) VALUES('Q01','Quầy 1','ACTIVE'),('Q02','Quầy 2','ACTIVE')
ON DUPLICATE KEY UPDATE code=VALUES(code);
INSERT INTO product_categories(code,name,status) VALUES
 ('UNCLASSIFIED','Chưa phân loại','ACTIVE'),
 ('DAIRY','Sữa và trứng','ACTIVE'),('FOOD','Thực phẩm','ACTIVE'),('DRINK','Đồ uống','ACTIVE')
ON DUPLICATE KEY UPDATE code=VALUES(code);
INSERT INTO units(code,name,quantity_scale) VALUES('HOP','Hộp',0),('CHAI','Chai',0),('CAI','Cái',0),('KG','Kilôgam',3)
ON DUPLICATE KEY UPDATE code=VALUES(code);
