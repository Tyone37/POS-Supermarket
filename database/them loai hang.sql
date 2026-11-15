USE pos;
SET NAMES utf8mb4;


INSERT INTO product_categories (code, name, status)
VALUES ('UNCLASSIFIED', 'Chưa phân loại', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = 'Chưa phân loại',
    status = 'ACTIVE';

-- Bổ sung các nhóm hàng
INSERT INTO product_categories (code, name, status)
VALUES
    ('DAIRY',        'Sữa và trứng',             'ACTIVE'),
    ('FOOD',         'Thực phẩm',               'ACTIVE'),
    ('DRINK',        'Đồ uống',                 'ACTIVE'),
    ('VEGETABLE',    'Rau củ',                   'ACTIVE'),
    ('FRUIT',        'Trái cây',                 'ACTIVE'),
    ('MEAT',         'Thịt và gia cầm',          'ACTIVE'),
    ('SEAFOOD',      'Thủy hải sản',             'ACTIVE'),
    ('FROZEN',       'Thực phẩm đông lạnh',      'ACTIVE'),
    ('CANNED',       'Đồ hộp',                   'ACTIVE'),
    ('INSTANT',      'Thực phẩm ăn liền',        'ACTIVE'),
    ('RICE_GRAIN',   'Gạo và ngũ cốc',           'ACTIVE'),
    ('SEASONING',    'Gia vị và dầu ăn',         'ACTIVE'),
    ('BAKERY',       'Bánh mì và bánh tươi',     'ACTIVE'),
    ('SNACK',        'Bánh kẹo và đồ ăn vặt',    'ACTIVE'),
    ('PERSONAL_CARE','Chăm sóc cá nhân',         'ACTIVE'),
    ('CLEANING',     'Giặt giũ và vệ sinh nhà',  'ACTIVE'),
    ('HOUSEHOLD',    'Đồ gia dụng',              'ACTIVE'),
    ('BABY',         'Sản phẩm mẹ và bé',        'ACTIVE'),
    ('PET',          'Đồ dùng và thức ăn thú cưng', 'ACTIVE'),
    ('STATIONERY',   'Văn phòng phẩm',           'ACTIVE')
ON DUPLICATE KEY UPDATE code = product_categories.code;

-- Kiểm tra kết quả
SELECT id, code, name, status
FROM product_categories
ORDER BY name;