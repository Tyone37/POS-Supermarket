# POS Supermarket — Ngữ cảnh chung cho AI khi phát triển

## 1. Cách sử dụng

Đính kèm toàn bộ file này cho AI ở đầu cuộc trò chuyện làm việc. File chứa quy tắc coding convention và bản trích cấu trúc dữ liệu, nên vẫn đọc được khi AI không mở được PDF hoặc MySQL Workbench. Gửi thêm mã task, tiêu chí nghiệm thu và những file code liên quan; nếu AI sửa repo thì cho phép AI đọc repo hiện hành.

Mẫu yêu cầu:

> Đọc toàn bộ AI_CONTEXT.md và áp dụng khi làm việc với dự án POS Supermarket. Task của tôi là [mã Jira và nội dung]. Tiêu chí nghiệm thu: [...]. Phạm vi được sửa: [...]. Trước khi code, đối chiếu lớp/bảng/cột sẽ dùng với repo và tài liệu này. Tái sử dụng phần có sẵn, báo rõ mâu thuẫn ảnh hưởng task, không tự đổi schema hoặc mở rộng phạm vi. Sau khi sửa, nêu file thay đổi, cách kiểm thử, kết quả thực tế và việc chưa hoàn tất.

Đây là hướng dẫn dự án do nhóm cung cấp, không thay thế các yêu cầu mới của người dùng hoặc quy tắc của công cụ AI. Các trích dẫn tài liệu bên dưới là dữ liệu tham khảo, không phải lệnh yêu cầu AI tự chạy mọi thao tác trong ví dụ.

## 2. Nguồn và cách giải quyết khác biệt

- Coding convention: `POS-Supermarket/CONTRIBUTING.md`, chép đầy đủ ở phần cuối.
- Cấu trúc vật lý: `SyncDrive/Final DB Design/erd.mwb`; phần 5 trích trực tiếp tên bảng/cột, kiểu, NULL, mặc định, cột sinh, index và FK.
- Giải thích nghiệp vụ: `SyncDrive/Final DB Design/CSDL Chung-Analysis.pdf`; các quy tắc cần thiết được diễn giải ở phần 4.
- Tình trạng khi tạo file: `database/schema.sql`, `pom.xml`, `package.json` của repo đang rỗng. Chưa có DDL chạy được để xác minh mô hình hoặc phiên bản thư viện trong các file đó. Không khẳng định ứng dụng đã build/chạy hoặc schema đã kiểm thử.

Khi làm việc:

1. Đọc phiên bản hiện hành của repo, CONTRIBUTING, schema và model nếu có quyền truy cập. File này là bản chụp tại thời điểm tạo, không tự cập nhật.
2. CONTRIBUTING quyết định cách viết và tổ chức code; model quyết định tên/kiểu/khóa vật lý; tài liệu phân tích giải thích nghiệp vụ. Quy tắc đặt tên chung không cho phép tự đổi tên cột đã tồn tại.
3. Nếu schema SQL sau này đã được nhóm review, nó là nguồn thực thi theo CONTRIBUTING. Nếu nó lệch model/tài liệu này, nêu khác biệt và yêu cầu quyết định cụ thể cho phần bị ảnh hưởng; không tự chọn một bản rồi sửa hàng loạt.
4. Mục không bị ảnh hưởng vẫn tiếp tục làm. Mọi đổi bảng dùng chung cần phương án migration, cập nhật model/tài liệu và PR được người phụ trách review.
5. Các tài liệu tổng hợp tuần trước chỉ là lịch sử; không lấy tên `sales_orders`, `sales_items`, `ORDER_LINE`, `product_id` làm PK thay `products.id` khi bản hiện hành bên dưới đã quy định khác.

## 3. Hợp đồng triển khai chung

- Stack: Java + Spring Boot + Maven; Thymeleaf + Bootstrap + TypeScript; MySQL. Không tự chuyển sang React, Node backend hay hệ CSDL khác. Phiên bản phải theo manifest đã chốt; nếu manifest trống, ghi rõ cần khởi tạo chung, không tự coi phiên bản mình chọn là chuẩn của nhóm.
- Package gốc `pos`; giữ các tầng `controller`, `service`, `repository`, `entity`, `dto`, `config`, `exception`, `util`. Controller gọi Service, Service gọi Repository; nghiệp vụ nằm trong Service.
- Inject bằng constructor với thuộc tính `private final`. Entity ánh xạ tường minh tên bảng/cột; không trả Entity trực tiếp qua API.
- Java thụt 4 spaces; TS/HTML/CSS 2 spaces; UTF-8; tên code tiếng Anh, UI tiếng Việt. Tên lớp PascalCase, biến/hàm camelCase, hằng/enum UPPER_SNAKE_CASE; file frontend kebab-case.
- Form dùng `@Controller`; JSON dùng `XxxApiController` với prefix `/api/`. Dùng DTO + `@Valid`; Service kiểm tra nghiệp vụ/quyền/phạm vi bản ghi. Không tin tổng tiền, giá, vai trò hay ca do client tự gửi.
- Không tắt CSRF để request chạy được, không nối chuỗi SQL từ đầu vào, không log secret, không commit cấu hình cá nhân.
- JPA dùng `ddl-auto=validate`; không dùng `update/create/create-drop` để âm thầm sửa schema dùng chung. Chỉ chạy script tạo bảng trên DB thử riêng khi có yêu cầu phù hợp.
- Kiểu ID SQL là BIGINT UNSIGNED. Java có thể dùng Long trong miền 0..Long.MAX_VALUE mà nhóm ứng dụng hỗ trợ; không tuyên bố Long bao phủ toàn dải UNSIGNED. Không gửi ID vào API dưới dạng JS number nếu có nguy cơ vượt Number.MAX_SAFE_INTEGER; thống nhất DTO dạng chuỗi khi cần.
- Tiền, phần trăm và lượng thập phân dùng BigDecimal, không dùng float/double. Giao tiếp JSON phải thống nhất biểu diễn tiền với frontend; không tự rải cách chuyển đổi ở từng module.
- DATETIME(3) trong model lưu UTC theo hợp đồng ứng dụng; bản thân DATETIME không có múi giờ. Java dùng java.time, đồng bộ JDBC/session và quy tắc chuyển Instant ↔ DATETIME UTC; UI hiển thị Asia/Ho_Chi_Minh.
- Không tự thêm trường Entity không có trong model (ví dụ @Version, audit, thuế, coupon) nếu chưa có thay đổi schema được review. Nếu cần khóa đồng thời, đề xuất cách phù hợp với cấu trúc hiện tại.
- Không tự động commit/push/merge hoặc đánh dấu Jira hoàn thành khi người dùng chỉ yêu cầu code. Không khẳng định người khác đã review.

## 4. Quy tắc nghiệp vụ và phạm vi

### 4.1 Tên và trạng thái dùng chung

| Đối tượng | Trạng thái và phạm vi theo tài liệu phân tích |
|---|---|
| accounts | ACTIVE, LOCKED |
| employees, registers, product_categories | ACTIVE, INACTIVE |
| products | ACTIVE, STOPPED; stopped_at đi cùng trạng thái |
| shifts | OPEN, CLOSED; chưa mở ca là trạng thái suy ra, không tạo ca rỗng |
| orders | DRAFT, PENDING, PAID, CANCELLED |
| payments | PENDING, SUCCESS, FAILED; method CASH trong phạm vi bản này |
| promotions | lifecycle_status ENABLED, CANCELED; hiệu lực theo starts_at/ends_at |
| invoices | ISSUED; VOID được dự trữ, chưa có quy trình hủy sau phát hành |
| inventory_movements | ADJUSTMENT, SALE; chưa có luồng RETURN được chốt |

Các trạng thái trên được mô tả trong PDF; không có CHECK/enum tương ứng được ghi trong mô hình đang đọc. Không coi việc cột VARCHAR có mặc định là đã giới hạn được các giá trị này ở DB. `CANCELED` của promotions và `CANCELLED` của orders là hai cách viết hiện có: ánh xạ chính xác, không tự đổi spelling chung.

### 4.2 Tài khoản và ca

- Một account có một role; role có nhiều permissions qua role_permissions. Một account có tối đa một employees; một employees bắt buộc gắn một account.
- Chỉ account ACTIVE, phiên còn hạn/chưa thu hồi và có quyền phù hợp được thao tác. Mật khẩu băm; login_sessions lưu token_hash, không lưu token gốc.
- Mỗi nhân viên tối đa một ca OPEN, được hỗ trợ bằng unique trên cột sinh open_employee_id. Không dùng UNIQUE(employee_id,status), vì sẽ chặn nhiều ca CLOSED.
- Tạo đơn/checkout phải gắn ca OPEN thuộc người thao tác. Chặn checkout đua với đóng ca bằng transaction/khóa phù hợp. Model chưa giới hạn mỗi quầy chỉ một ca OPEN; không tự thêm quy tắc này.
- OPEN: closed_at và các số liệu closing_* là NULL. CLOSED: closed_at >= opened_at; số liệu chốt đầy đủ; closing_expected_cash = opening_cash + closing_cash_sales; closing_difference = closing_actual_cash - closing_expected_cash. Chênh lệch được âm.

### 4.3 Sản phẩm và khuyến mãi

- Dùng products.id làm FK; code và barcode là mã nghiệp vụ riêng, đều UNIQUE. Không dùng barcode làm PK; giữ số 0 đầu.
- Giá > 0; stock_quantity >= 0; 0 <= reserved_quantity <= stock_quantity. Đơn vị quantity_scale = 0 hoặc 3 theo phân tích; không tự mở rộng hàng cân nếu task không yêu cầu.
- Một sản phẩm một category và unit. Không sửa đơn vị sau khi có lịch sử nếu chưa có quy tắc chuyển đổi được duyệt.
- Khuyến mãi phần trăm: 0 < discount_percent <= 100; starts_at < ends_at; không trùng lịch trên cùng sản phẩm. Kiểm tra overlap và thao tác đồng thời cần Service/khóa, FK không kiểm tra được.
- Ngừng bán giữ lịch sử; không xóa dữ liệu được order_items tham chiếu. Trong model hiện hành promotion_products.product_id là RESTRICT, không lấy CASCADE từ thiết kế cũ.

### 4.4 Đơn hàng thanh toán hóa đơn và tồn

- Một orders có nhiều order_items; UNIQUE(order_id,product_id) nghĩa là gộp cùng sản phẩm thành một dòng. Đơn đã trả tiền phải có ít nhất một dòng; FK không bảo đảm điều kiện này.
- order_items.discount_amount là tổng giảm của dòng, không phải giảm một đơn vị. Giữ snapshot mã/tên/mã vạch/đơn vị/giá/khuyến mãi lúc bán. Không đọc lại giá hiện hành để in hóa đơn cũ.
- Công thức đề xuất trong phân tích: grossLine = quantity × unit_price, làm tròn HALF_UP 2 chữ số; line_total = grossLine - discount_amount. Tổng trước giảm = tổng grossLine; tổng giảm = tổng discount dòng; total_amount = subtotal_amount - discount_amount = tổng line_total. Chưa hỗ trợ giảm toàn đơn/thuế/phí.
- Cách tính discount_amount từ phần trăm và thời điểm làm tròn so với cách tính giá sau giảm trên mỗi đơn vị còn cần Tú–Tín xác nhận. Không âm thầm chọn thuật toán riêng; dùng một hàm chung có test sau khi chốt.
- Một đơn nhiều lần thử payments nhưng tối đa một SUCCESS và một PENDING (các cột sinh UNIQUE hỗ trợ từng điều kiện riêng). Không coi hai unique này là cấm đồng thời một PENDING và một SUCCESS: Service phải chuyển trạng thái đúng.
- SUCCESS có tiền nhận đủ, change_amount = amount_received - amount_due; FAILED/PENDING chưa ghi nhận tiền trong hợp đồng này. Thanh toán tiền mặt là phạm vi nền; không tự xây QR/thẻ/ví.
- Checkout dùng một transaction: kiểm tra/khóa ca, đơn và tồn theo thứ tự chung; tính lại tiền; giữ snapshot; ghi SUCCESS, PAID, hóa đơn, trừ tồn và SALE. Một bước lỗi thì rollback toàn bộ. Quy tắc giữ/nhả reserved_quantity với PENDING phải được thống nhất trước khi dùng.
- invoices.order_id và payment_id đều UNIQUE. FK ghép (payment_id,order_id) → payments(id,order_id) bảo đảm cùng đơn; Service vẫn kiểm tra payment SUCCESS, order PAID và amount_due = orders.total_amount = invoices.total_amount.
- Chống gửi lặp bằng payments.idempotency_key và inventory_movements.event_key, kiểm tra payload khác với cùng key. Không tạo hóa đơn/trừ kho lần hai. Khóa ngoại không tự bảo vệ bất biến snapshot hoặc tổng nhiều dòng.
- Sau PAID không sửa/xóa dòng hoặc tổng lịch sử; in lại chỉ đọc. Không tự thêm invoice_items song song vì model đã dùng order_items chứa snapshot.
- Trả/đổi, hoàn tiền, hủy hóa đơn sau phát hành và điều chỉnh sau đóng ca chưa có chứng từ/quy tắc đầy đủ. Nếu task yêu cầu, nêu thiết kế cần bổ sung; không sửa hóa đơn gốc hoặc tự trừ doanh thu.

### 4.5 Báo cáo dùng lại nguồn hiện có

- Không tạo bảng lưu các chỉ số báo cáo khi task chỉ yêu cầu lọc/tổng hợp.
- Tập hóa đơn hợp lệ: invoices ISSUED, order PAID, payment SUCCESS thuộc cùng đơn. Thời điểm ghi nhận dự kiến invoices.issued_at; đối chiếu story hiện hành trước triển khai.
- Lọc ngày Việt Nam từ A đến B bằng mốc UTC [00:00 A, 00:00 ngày sau B); không dùng cuối ngày 23:59:59. Nhóm ngày theo giờ Việt Nam, gồm cả ngày không có giao dịch.
- Doanh thu = SUM(invoices.total_amount); số hóa đơn = số invoices.id hợp lệ; trung bình = doanh thu/số hóa đơn, rỗng trả 0. Không cộng opening_cash, amount_received hay change_amount.
- Sản phẩm bán nhiều dùng order_items của tập hóa đơn hợp lệ: SUM(quantity), SUM(line_total), giữ snapshot và phân biệt đơn vị. Không cộng tổng hóa đơn sau join chi tiết hoặc nối tất cả lần thử payments gây nhân dòng.
- Danh sách hóa đơn đối chiếu dùng chính xác cùng bộ lọc/quyền; tổng hóa đơn = tổng theo ngày = tổng dòng trong phạm vi công thức trên. Không khẳng định doanh thu thuần sau trả khi chưa có dữ liệu trả.

## 5. Cấu trúc trích trực tiếp từ MySQL Workbench

Các bảng dưới là bản chụp cấu trúc, không phải script đã thực thi. NULL/mặc định/index/FK là dữ liệu của model; các quy tắc nghiệp vụ bổ sung ở phần 4 phải được xử lý và kiểm thử riêng. Cột sinh dùng insertable=false, updatable=false khi ánh xạ JPA, không nhận từ client.

### `accounts`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `username` | `VARCHAR(100)` | không | không khai báo |
| `password_hash` | `VARCHAR(255)` | không | không khai báo |
| `full_name` | `VARCHAR(200)` | không | không khai báo |
| `role_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ACTIVE'` |
| `created_at` | `DATETIME(3)` | không | không khai báo |
| `updated_at` | `DATETIME(3)` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `username` — UNIQUE (`username`).
- `role_id` — INDEX (`role_id`).

**Khóa ngoại:**

- `(role_id)` → `roles(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `employees`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `account_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `employee_code` | `VARCHAR(30)` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ACTIVE'` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `account_id` — UNIQUE (`account_id`).
- `employee_code` — UNIQUE (`employee_code`).

**Khóa ngoại:**

- `(account_id)` → `accounts(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `inventory_movements`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `event_key` | `VARCHAR(64)` | không | không khai báo |
| `product_id` | `BIGINT UNSIGNED` | có | DEFAULT `NULL` |
| `product_code_snapshot` | `VARCHAR(20)` | không | không khai báo |
| `order_item_id` | `BIGINT UNSIGNED` | có | DEFAULT `NULL` |
| `movement_type` | `VARCHAR(10)` | không | không khai báo |
| `quantity_delta` | `DECIMAL(15,3)` | không | không khai báo |
| `reason` | `VARCHAR(255)` | không | không khai báo |
| `created_at` | `DATETIME(3)` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `event_key` — UNIQUE (`event_key`).
- `product_id` — INDEX (`product_id`).
- `order_item_id` — INDEX (`order_item_id`).

**Khóa ngoại:**

- `(product_id)` → `products(id)`; ON DELETE SET NULL; ON UPDATE không khai báo trong model.
- `(order_item_id)` → `order_items(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `invoices`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `invoice_no` | `VARCHAR(30)` | không | không khai báo |
| `order_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `payment_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `issued_by` | `BIGINT UNSIGNED` | không | không khai báo |
| `issued_at` | `DATETIME(3)` | không | không khai báo |
| `subtotal_amount` | `DECIMAL(18,2)` | không | không khai báo |
| `discount_amount` | `DECIMAL(18,2)` | không | không khai báo |
| `total_amount` | `DECIMAL(18,2)` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ISSUED'` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `invoice_no` — UNIQUE (`invoice_no`).
- `order_id` — UNIQUE (`order_id`).
- `payment_id` — UNIQUE (`payment_id`).
- `idx_invoice_status_date` — INDEX (`status, issued_at`).
- `payment_id_2` — INDEX (`payment_id, order_id`).
- `issued_by` — INDEX (`issued_by`).

**Khóa ngoại:**

- `(order_id)` → `orders(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(payment_id, order_id)` → `payments(id, order_id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(issued_by)` → `accounts(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `login_sessions`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `account_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `token_hash` | `VARCHAR(255)` | không | không khai báo |
| `created_at` | `DATETIME(3)` | không | không khai báo |
| `expires_at` | `DATETIME(3)` | không | không khai báo |
| `revoked_at` | `DATETIME(3)` | có | DEFAULT `NULL` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `token_hash` — UNIQUE (`token_hash`).
- `idx_sessions_account` — INDEX (`account_id`).

**Khóa ngoại:**

- `(account_id)` → `accounts(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `order_items`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `order_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `product_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `product_code_snapshot` | `VARCHAR(20)` | không | không khai báo |
| `product_name_snapshot` | `VARCHAR(200)` | không | không khai báo |
| `barcode_snapshot` | `VARCHAR(50)` | không | không khai báo |
| `unit_code_snapshot` | `VARCHAR(12)` | không | không khai báo |
| `quantity` | `DECIMAL(15,3)` | không | không khai báo |
| `unit_price` | `DECIMAL(18,2)` | không | không khai báo |
| `promotion_id` | `BIGINT UNSIGNED` | có | DEFAULT `NULL` |
| `promotion_code_snapshot` | `VARCHAR(20)` | có | DEFAULT `NULL` |
| `promotion_name_snapshot` | `VARCHAR(200)` | có | DEFAULT `NULL` |
| `discount_amount` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `line_total` | `DECIMAL(18,2)` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `uq_item_product` — UNIQUE (`order_id, product_id`).
- `product_id` — INDEX (`product_id`).
- `promotion_id` — INDEX (`promotion_id`).

**Khóa ngoại:**

- `(order_id)` → `orders(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(product_id)` → `products(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(promotion_id)` → `promotions(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `orders`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `order_no` | `VARCHAR(30)` | không | không khai báo |
| `shift_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `status` | `VARCHAR(12)` | không | DEFAULT `'DRAFT'` |
| `subtotal_amount` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `discount_amount` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `total_amount` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `created_at` | `DATETIME(3)` | không | không khai báo |
| `paid_at` | `DATETIME(3)` | có | DEFAULT `NULL` |
| `cancelled_at` | `DATETIME(3)` | có | DEFAULT `NULL` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `order_no` — UNIQUE (`order_no`).
- `idx_orders_shift_status` — INDEX (`shift_id, status`).

**Khóa ngoại:**

- `(shift_id)` → `shifts(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `payments`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `order_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `method` | `VARCHAR(12)` | không | DEFAULT `'CASH'` |
| `status` | `VARCHAR(10)` | không | không khai báo |
| `amount_due` | `DECIMAL(18,2)` | không | không khai báo |
| `amount_received` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `change_amount` | `DECIMAL(18,2)` | không | DEFAULT `'0.00'` |
| `idempotency_key` | `VARCHAR(64)` | không | không khai báo |
| `created_at` | `DATETIME(3)` | không | không khai báo |
| `completed_at` | `DATETIME(3)` | có | DEFAULT `NULL` |
| `success_order_id` | `BIGINT UNSIGNED` | có | GENERATED STORED: `(case when (`status` = _utf8mb4'SUCCESS') then `order_id` else NULL end)` |
| `pending_order_id` | `BIGINT UNSIGNED` | có | GENERATED STORED: `(case when (`status` = _utf8mb4'PENDING') then `order_id` else NULL end)` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `idempotency_key` — UNIQUE (`idempotency_key`).
- `uq_payment_order` — UNIQUE (`id, order_id`).
- `uq_payment_success` — UNIQUE (`success_order_id`).
- `uq_payment_pending` — UNIQUE (`pending_order_id`).
- `order_id` — INDEX (`order_id`).

**Khóa ngoại:**

- `(order_id)` → `orders(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `permissions`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(60)` | không | không khai báo |
| `description` | `VARCHAR(255)` | có | DEFAULT `NULL` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

### `product_categories`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(20)` | không | không khai báo |
| `name` | `VARCHAR(100)` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ACTIVE'` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

### `products`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(20)` | không | không khai báo |
| `barcode` | `VARCHAR(50)` | không | không khai báo |
| `name` | `VARCHAR(200)` | không | không khai báo |
| `category_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `unit_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `sale_price` | `DECIMAL(18,2)` | không | không khai báo |
| `stock_quantity` | `DECIMAL(15,3)` | không | DEFAULT `'0.000'` |
| `reserved_quantity` | `DECIMAL(15,3)` | không | DEFAULT `'0.000'` |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ACTIVE'` |
| `stopped_at` | `DATETIME(3)` | có | DEFAULT `NULL` |
| `created_at` | `DATETIME(3)` | không | không khai báo |
| `updated_at` | `DATETIME(3)` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).
- `barcode` — UNIQUE (`barcode`).
- `category_id` — INDEX (`category_id`).
- `unit_id` — INDEX (`unit_id`).

**Khóa ngoại:**

- `(category_id)` → `product_categories(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(unit_id)` → `units(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `promotion_products`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `promotion_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `product_id` | `BIGINT UNSIGNED` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`promotion_id, product_id`).
- `product_id` — INDEX (`product_id`).

**Khóa ngoại:**

- `(promotion_id)` → `promotions(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(product_id)` → `products(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `promotions`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(20)` | không | không khai báo |
| `name` | `VARCHAR(200)` | không | không khai báo |
| `discount_percent` | `DECIMAL(5,2)` | không | không khai báo |
| `starts_at` | `DATETIME(3)` | không | không khai báo |
| `ends_at` | `DATETIME(3)` | không | không khai báo |
| `lifecycle_status` | `VARCHAR(10)` | không | DEFAULT `'ENABLED'` |
| `canceled_at` | `DATETIME(3)` | có | DEFAULT `NULL` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

### `registers`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(30)` | không | không khai báo |
| `name` | `VARCHAR(100)` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'ACTIVE'` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

### `role_permissions`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `role_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `permission_id` | `BIGINT UNSIGNED` | không | không khai báo |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`role_id, permission_id`).
- `permission_id` — INDEX (`permission_id`).

**Khóa ngoại:**

- `(role_id)` → `roles(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(permission_id)` → `permissions(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `roles`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(40)` | không | không khai báo |
| `name` | `VARCHAR(100)` | không | không khai báo |
| `description` | `VARCHAR(255)` | có | DEFAULT `NULL` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

### `shifts`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `shift_no` | `VARCHAR(30)` | không | không khai báo |
| `employee_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `register_id` | `BIGINT UNSIGNED` | không | không khai báo |
| `status` | `VARCHAR(10)` | không | DEFAULT `'OPEN'` |
| `open_request_key` | `VARCHAR(64)` | không | không khai báo |
| `opened_at` | `DATETIME(3)` | không | không khai báo |
| `closed_at` | `DATETIME(3)` | có | DEFAULT `NULL` |
| `opening_cash` | `DECIMAL(18,2)` | không | không khai báo |
| `closing_revenue` | `DECIMAL(18,2)` | có | DEFAULT `NULL` |
| `closing_cash_sales` | `DECIMAL(18,2)` | có | DEFAULT `NULL` |
| `closing_expected_cash` | `DECIMAL(18,2)` | có | DEFAULT `NULL` |
| `closing_actual_cash` | `DECIMAL(18,2)` | có | DEFAULT `NULL` |
| `closing_difference` | `DECIMAL(18,2)` | có | DEFAULT `NULL` |
| `open_employee_id` | `BIGINT UNSIGNED` | có | GENERATED STORED: `(case when (`status` = _utf8mb4'OPEN') then `employee_id` else NULL end)` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `shift_no` — UNIQUE (`shift_no`).
- `open_request_key` — UNIQUE (`open_request_key`).
- `uq_open_employee` — UNIQUE (`open_employee_id`).
- `employee_id` — INDEX (`employee_id`).
- `register_id` — INDEX (`register_id`).

**Khóa ngoại:**

- `(employee_id)` → `employees(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.
- `(register_id)` → `registers(id)`; ON DELETE RESTRICT; ON UPDATE không khai báo trong model.

### `units`

| Cột | Kiểu SQL | NULL | Mặc định và thuộc tính |
|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | không | AUTO_INCREMENT |
| `code` | `VARCHAR(12)` | không | không khai báo |
| `name` | `VARCHAR(50)` | không | không khai báo |
| `quantity_scale` | `TINYINT UNSIGNED` | không | DEFAULT `'0'` |

**Khóa và index có trong model:**

- `PRIMARY` — PRIMARY (`id`).
- `code` — UNIQUE (`code`).

## 6. Quy trình AI thực hiện một task

1. Tóm tắt phạm vi và tiêu chí nghiệm thu; chỉ ra file code/bảng/cột/trạng thái liên quan.
2. Tìm implementation, DTO, cấu hình, exception và test chung hiện có. Tái sử dụng trước khi tạo mới; không tự dựng cả dự án khi chỉ sửa một tính năng.
3. Báo mâu thuẫn ảnh hưởng trực tiếp task trước thay đổi schema hoặc giao diện tích hợp. Không bịa cột, endpoint hay kết quả kiểm thử.
4. Viết thay đổi nhỏ, đúng tầng; cập nhật DTO/frontend/test đồng bộ nếu hợp đồng thay đổi. SQL có tham số, không rò rỉ dữ liệu hoặc quyền.
5. Chạy build/test phù hợp với manifest thật. Không chạy npm ci khi chưa có lockfile; không tuyên bố mvn test đạt khi pom.xml còn trống. Ghi chính xác lệnh và giới hạn nếu không chạy được.
6. Báo kết quả: chức năng đã làm, file sửa, bảng/cột dùng, thay đổi hợp đồng, test đã chạy/kết quả và mục cần nhóm xác nhận. Không tự merge/push/Jira.

### Checklist dành cho AI trước bàn giao

- [ ] Đúng task, đúng stack, đúng package, đúng format của CONTRIBUTING.
- [ ] Không có bảng/cột tự đoán hoặc bộ dữ liệu trùng; Entity khớp tên/kiểu/null/index trong nguồn hiện hành.
- [ ] Cột sinh không ghi từ JPA/client; ID, BigDecimal và thời gian được ánh xạ đúng.
- [ ] Validation, quyền và nghiệp vụ kiểm tra ở server; lỗi theo cấu trúc chung.
- [ ] Tính tiền/snapshot/rollback/chống lặp được kiểm tra nếu thay đổi checkout.
- [ ] Build/test được ghi theo thực tế, không dùng phép tính mẫu thay bằng chứng integration.
- [ ] Không secret/debug/file sinh; đổi schema có migration và review phù hợp.

## 7. Toàn văn coding convention của nhóm

Nội dung dưới đây được sao từ CONTRIBUTING.md để thành viên chỉ cần import một file. Khi CONTRIBUTING thay đổi, cần cập nhật bản sao và dấu vân tay nguồn ở cuối tài liệu.

## Coding conventions và quy ước đóng góp

Tài liệu này thống nhất cách viết code và phối hợp trong dự án POS Supermarket. Mọi thành viên áp dụng khi viết mới hoặc sửa code. Thay đổi quy ước bằng Pull Request (PR) để nhóm review.

### 1. Công nghệ và cấu trúc dự án

| Thành phần | Quy ước |
|---|---|
| Backend | Java, Spring Boot, Maven; Controller → Service → Repository |
| Frontend | Thymeleaf, Bootstrap, TypeScript biên dịch thành JavaScript |
| CSDL | MySQL |
| Kiểm thử | JUnit cho nghiệp vụ; Postman cho API JSON |

Phiên bản Java/Spring Boot được khai báo trong `pom.xml`; TypeScript trong `package.json` và lockfile. README phải ghi phiên bản JDK, Node.js và MySQL đã dùng để chạy thử. Khi khởi tạo dự án, người phụ trách cấu hình phải hoàn thiện các thông tin này trước khi nhóm tích hợp code. Không tự nâng phiên bản hoặc thêm thư viện khi chưa review tác động qua PR.

```text
pos-supermarket/
├── README.md
├── CONTRIBUTING.md
├── pom.xml
├── package.json
├── package-lock.json
├── tsconfig.json
├── docs/
│   ├── requirements/             # backlog, user story
│   ├── design/                   # UML, ERD, hợp đồng dữ liệu
│   └── postman/                  # collection và environment mẫu
├── database/
│   ├── schema.sql
│   └── seed.sql
└── src/
    ├── main/
    │   ├── java/pos/
    │   │   ├── PosApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   ├── entity/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   └── util/
    │   ├── ts/                   # chia theo tính năng
    │   └── resources/
    │       ├── application.properties
    │       ├── application-example.properties
    │       ├── templates/        # layout/, auth/, product/, sale/, ...
    │       └── static/           # css/, img/, js/ (JS được biên dịch)
    └── test/java/pos/
```

### 2. Định dạng và cách trình bày code

- File dùng UTF-8; kết thúc file bằng một dòng mới; không để khoảng trắng cuối dòng.
- Java dùng 4 dấu cách; TypeScript, HTML và CSS dùng 2 dấu cách. Không dùng tab để thụt lề.
- Giới hạn dòng khoảng 120 ký tự; xuống dòng tại tham số hoặc chuỗi gọi hàm dài để dễ đọc.
- Dấu `{` đặt cùng dòng khai báo; luôn dùng `{}` cho `if`, `else`, `for`, `while`, kể cả chỉ có một lệnh.
- Có dấu cách quanh toán tử và sau dấu phẩy; dùng dòng trống giữa các phương thức và nhóm xử lý.
- Mỗi file Java chỉ có một kiểu public, tên file trùng tên kiểu. Import tường minh; không dùng wildcard, xóa import không dùng.
- Sắp xếp thành viên Java: hằng số → thuộc tính → constructor → phương thức public → phương thức hỗ trợ private.
- Dùng formatter của IDE theo các quy tắc trên. Chỉ format phần đang sửa, tránh tạo diff toàn file không liên quan.
- Hàm làm một việc rõ ràng; tách validation, tính toán hoặc truy vấn khi hàm có quá nhiều trách nhiệm. Ưu tiên return sớm để giảm lồng điều kiện.
- Không để code chết, code bị comment, biến không dùng hoặc giá trị nghiệp vụ rải rác. Đưa giá trị có ý nghĩa vào hằng số hoặc cấu hình.
- Comment giải thích lý do hoặc ràng buộc khó hiểu; không lặp lại câu lệnh. Javadoc cho API dùng chung và phép tính nghiệp vụ cần giải thích. TODO phải kèm mã task Jira.

```java
public BigDecimal calculateChange(BigDecimal received, BigDecimal total) {
    if (received.compareTo(total) < 0) {
        throw new InsufficientPaymentException("Số tiền khách đưa chưa đủ.");
    }

    return received.subtract(total);
}
```

### 3. Quy tắc đặt tên

Tên trong code dùng tiếng Anh, có nghĩa theo nghiệp vụ; không dùng tên như `data1`, `abc`, `handleAll`. Nội dung hiển thị cho người dùng dùng tiếng Việt có dấu.

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Package Java | chữ thường, dưới `pos` | `pos.service` |
| Lớp, interface, enum | PascalCase; enum là danh từ số ít | `ProductService`, `OrderStatus` |
| Hàm, biến, tham số | camelCase; hàm bắt đầu bằng động từ | `findByBarcode`, `totalAmount` |
| Boolean | diễn đạt điều kiện | `active`, `hasOpenShift`, `canCheckout` |
| Hằng số, giá trị enum | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `PAID` |
| Controller/Service/Repository | tên nghiệp vụ + vai trò | `ProductController`, `ProductRepository` |
| DTO | tên nghiệp vụ + mục đích | `CreateProductRequest`, `ProductResponse` |
| Bảng MySQL | snake_case, danh từ số nhiều | `products`, `order_items` |
| Cột MySQL | snake_case; khóa ngoại kết thúc `_id` | `sale_price`, `product_id` |
| HTML/TypeScript/CSS | kebab-case; thư mục theo tính năng | `product/list.html`, `sale/cart-form.ts` |
| Class CSS | kebab-case | `.product-card` |
| Test | tên lớp + Test; hàm nêu hành vi và điều kiện | `PaymentServiceTest`, `checkout_shouldRejectClosedShift` |

Entity dùng danh từ số ít (`Product`), ánh xạ rõ tên bảng/cột khi khác tên Java. Tên bảng, khóa chính và kiểu dữ liệu cụ thể phải theo schema chung được review; không tự tạo thêm bảng tương đương cho cùng nghiệp vụ.

### 4. Trách nhiệm từng lớp và Java/Spring

| Lớp | Trách nhiệm | Giới hạn |
|---|---|---|
| View | Hiển thị, nhận thao tác, validation hỗ trợ nhập liệu | Không quyết định giá, quyền hoặc kết quả thanh toán |
| Controller | Nhận request, validate DTO, gọi Service, chọn view/trả response | Không gọi trực tiếp Repository, không tính tiền |
| Service | Kiểm tra nghiệp vụ, quyền/phạm vi dữ liệu phù hợp, điều phối transaction | Không phụ thuộc HTML, HttpServletRequest hoặc ResponseEntity |
| Repository | Truy vấn và lưu dữ liệu | Không điều phối nghiệp vụ |
| Entity | Ánh xạ và bảo vệ trạng thái dữ liệu của đối tượng | Không xử lý HTTP hoặc điều phối nhiều repository |
| DTO | Dữ liệu vào/ra theo từng thao tác | Không chứa mật khẩu băm hay trường nội bộ không cần hiển thị |

- Inject dependency qua constructor, dùng thuộc tính `private final`; không field injection bằng `@Autowired`.
- Thuộc tính để `private`; chỉ mở phương thức cần dùng. Không tạo lớp `util` để gom nghiệp vụ vốn thuộc Service.
- Dùng enum cho tập trạng thái cố định; không rải chuỗi trạng thái trong code.
- Repository có thể trả `Optional<T>` khi tìm một bản ghi; Service xử lý trường hợp không tồn tại rõ ràng. Không gọi `Optional.get()` khi chưa kiểm tra.
- Collection trả về rỗng khi không có phần tử. Không dùng `null` để biểu diễn danh sách rỗng.
- Đọc dữ liệu entity cần thiết và ánh xạ DTO trong Service; không dựa vào việc template tự tải quan hệ lazy.
- Không trả trực tiếp Entity qua API; tránh lộ trường nội bộ và vòng lặp quan hệ.

### 5. Validation, phân quyền và xử lý lỗi

- DTO đầu vào khai báo ràng buộc phù hợp như `@NotBlank`, `@Size`, `@Positive`; Controller kích hoạt bằng `@Valid`.
- Service kiểm tra điều kiện nghiệp vụ: mã vạch trùng, tồn khả dụng, ca đang mở, quyền thao tác, trạng thái đơn và số tiền khách đưa.
- Mọi dữ liệu từ trình duyệt đều cần kiểm tra ở máy chủ. Máy chủ tính lại giá, giảm giá và tổng tiền từ nguồn tin cậy; không tin tổng tiền hoặc vai trò gửi từ client.
- Phân quyền tại máy chủ bằng cơ chế bảo mật chung; kiểm tra cả quyền chức năng và quyền với bản ghi được yêu cầu. Ẩn nút chỉ hỗ trợ giao diện.
- Dùng truy vấn tham số hóa/JPA; không nối trực tiếp dữ liệu người dùng vào SQL.
- Không `catch (Exception)` rồi bỏ qua hoặc trả thành công. Chỉ bắt khi có thể xử lý, chuyển đổi lỗi hoặc bổ sung ngữ cảnh; giữ nguyên nguyên nhân khi bọc exception.
- Dùng exception có nghĩa như `ProductNotFoundException`, `InsufficientStockException`; xử lý tập trung bằng `@ControllerAdvice`/`@RestControllerAdvice` phù hợp loại Controller.
- Lỗi form trả lại trang nhập với lỗi tại trường và giữ dữ liệu hợp lệ. Lỗi API trả cấu trúc chung; không trả stack trace hoặc thông tin CSDL cho người dùng.

Cấu trúc lỗi API thống nhất:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Vui lòng kiểm tra dữ liệu nhập.",
  "fieldErrors": {
    "barcode": "Mã vạch không được để trống."
  }
}
```

`fieldErrors` là đối tượng rỗng nếu lỗi không gắn với trường. `code` là mã ổn định để frontend xử lý; `message` dành cho người dùng.

| HTTP status | Sử dụng |
|---|---|
| 400 | Dữ liệu đầu vào hoặc yêu cầu không hợp lệ |
| 401 | Chưa xác thực hoặc phiên hết hạn |
| 403 | Không có quyền |
| 404 | Không tìm thấy tài nguyên trong phạm vi được phép |
| 409 | Trùng dữ liệu hoặc xung đột trạng thái, như ca đã đóng |
| 500 | Lỗi ngoài dự kiến; thông báo chung, chi tiết ghi ở log |

### 6. Tiền tệ, thời gian và transaction

- Java dùng `BigDecimal` cho tiền, phần trăm và số lượng thập phân nếu nghiệp vụ hỗ trợ. Không dùng `float`/`double` để tính tiền; tạo giá trị từ chuỗi hoặc dữ liệu DECIMAL.
- So sánh giá trị `BigDecimal` bằng `compareTo`, không dùng `equals` khi chỉ quan tâm giá trị số.
- Tiền trong MySQL dùng `DECIMAL`, không dùng FLOAT/DOUBLE. Độ chính xác, số chữ số lẻ và thời điểm làm tròn phải được chốt trong thiết kế dữ liệu chung trước khi triển khai thanh toán.
- Quy tắc làm tròn phải nằm ở một nơi dùng chung, có test; không tự chọn mỗi module một cách. Định dạng dấu phân cách và ký hiệu VND chỉ thực hiện khi hiển thị.
- Không tự mở rộng sang hàng cân, thanh toán QR/thẻ hoặc hoàn tiền nếu story của sprint chưa chọn phạm vi đó.
- Thời điểm giao dịch lưu theo UTC, hiển thị theo `Asia/Ho_Chi_Minh`. Cấu hình ứng dụng, JDBC và CSDL phải thống nhất cách chuyển đổi; không dựa vào múi giờ mặc định của máy cá nhân.
- Java dùng `java.time`; dùng `Instant` cho thời điểm, `LocalDate` cho ngày nghiệp vụ. Truy vấn báo cáo theo khoảng đầu ngày đến đầu ngày kế tiếp, chuyển từ giờ Việt Nam sang UTC.
- Đặt `@Transactional` tại phương thức Service cho một thao tác ghi cần tính nguyên vẹn. Lưu thanh toán, dữ liệu hóa đơn và cập nhật tồn liên quan phải thành công cùng nhau hoặc rollback theo luồng nghiệp vụ đã chốt.
- Không bắt lỗi rồi nuốt exception bên trong transaction. Nếu dùng checked exception, khai báo rollback phù hợp.
- Kiểm tra tồn và trạng thái cùng cơ chế khóa/phiên bản phù hợp trong transaction; không chỉ kiểm tra ở frontend hoặc dựa vào thao tác đọc rồi ghi không được bảo vệ.
- Chống gửi lặp ở máy chủ bằng khóa nghiệp vụ/idempotency và ràng buộc UNIQUE phù hợp. Retry không tạo thêm hóa đơn hoặc trừ tồn lần hai.
- Không giữ transaction/khóa CSDL trong lúc gọi dịch vụ bên ngoài. Nếu sprint có tích hợp bên ngoài, thiết kế riêng luồng chờ, xác nhận và đối soát.
- Giá, tên hàng và giảm giá đã chốt phải được giữ trong dữ liệu lịch sử; sửa sản phẩm hoặc in lại hóa đơn không làm đổi giao dịch cũ.

### 7. Controller, API và truy vấn

- Trang Thymeleaf dùng `@Controller`; API JSON dùng `@RestController`, tên `XxxApiController`, prefix `/api/`.
- API dùng danh từ số nhiều: `GET /api/products`, `POST /api/products`, `PUT /api/products/{id}`. Thao tác nghiệp vụ dùng đường dẫn rõ như `POST /api/orders/{id}/checkout`.
- GET chỉ đọc; không dùng GET để xóa, thanh toán hoặc thay đổi trạng thái.
- Tạo mới trả 201; đọc/cập nhật có dữ liệu trả 200; thành công không có body trả 204. Không trả HTTP 200 cho lỗi.
- Form ghi dữ liệu thành công dùng redirect để tránh gửi lại khi refresh; chống thanh toán lặp vẫn phải thực hiện ở Service/CSDL.
- Danh sách có thể tăng lớn phải phân trang; giới hạn kích thước trang và cho phép sắp xếp trên danh sách trường được quy định.
- Tránh truy vấn trong vòng lặp gây N+1; lấy đủ dữ liệu bằng query/projection phù hợp. Chỉ thêm index dựa trên nhu cầu truy vấn cụ thể.

### 8. TypeScript, Thymeleaf và CSS

- TypeScript nguồn ở `src/main/ts/`, đầu ra ở `src/main/resources/static/js/`; không sửa trực tiếp JS đã biên dịch.
- Bật `strict` trong `tsconfig.json`; ưu tiên kiểu/interface rõ ràng, `const` mặc định và `let` khi cần gán lại. Không dùng `var`, `any` hoặc ép non-null để che lỗi kiểu; kiểm tra DOM/null trước khi dùng.
- Dùng dấu nháy đơn, dấu chấm phẩy và `===`/`!==`. Dùng `async/await`; xử lý lỗi mạng và kiểm tra `response.ok` trước khi coi request thành công.
- Không dùng tính toán JavaScript làm kết quả tiền cuối cùng. Giá trị máy chủ trả về là nguồn dùng để xác nhận thanh toán.
- Chặn bấm gửi lặp khi đang xử lý và bật lại khi lỗi; không coi việc khóa nút là thay thế chống gửi lặp phía máy chủ.
- Nội dung người dùng dùng `textContent` hoặc `th:text`; không đưa vào `innerHTML`/`th:utext` khi chưa có xử lý an toàn được review.
- Form có label gắn với input, thông báo lỗi rõ; nút không submit phải có `type="button"`. Không viết sự kiện JavaScript inline trong HTML.
- Header/sidebar dùng fragment chung trong `templates/layout/`. Tách CSS/TypeScript ra file theo tính năng; ưu tiên Bootstrap và class dùng chung, hạn chế style inline.
- Giữ cơ chế CSRF của ứng dụng cho form và request ghi dữ liệu; không tắt bảo vệ chỉ để request chạy được.

Các script cần có khi hoàn thiện cấu hình:

```json
{
  "scripts": {
    "build": "tsc",
    "watch": "tsc --watch"
  }
}
```

Commit `package-lock.json`; dùng `npm ci` khi đã có lockfile, sau đó `npm run build`. Khi chủ động đổi dependency, dùng `npm install` và commit cả manifest lẫn lockfile.

### 9. CSDL và cấu hình

- `database/schema.sql` là nguồn chuẩn cấu trúc bảng. ERD, Entity và tài liệu dữ liệu phải khớp schema sau mỗi thay đổi.
- Chốt tên bảng/khóa và bên sở hữu dữ liệu trước khi tích hợp. Không đồng thời tạo `sales_items` và `order_items` cho cùng một loại dữ liệu chỉ vì hai tài liệu dùng tên khác nhau.
- Khóa ngoại phải tương thích kiểu với khóa được tham chiếu, kể cả signed/unsigned. Khai báo rõ NOT NULL, UNIQUE, FK và CHECK phù hợp; kiểm tra nghiệp vụ nhiều dòng vẫn thuộc Service/transaction.
- Tài liệu thiết kế logic phải được chuyển sang DDL MySQL hợp lệ trước khi đưa vào schema.
- Dùng `spring.jpa.hibernate.ddl-auto=validate`; không dùng Hibernate tự sửa schema chung.
- Đổi schema bằng PR nhỏ, kèm hướng dẫn cập nhật dữ liệu hiện có và dữ liệu mẫu. Không yêu cầu cả nhóm chạy lại script có lệnh xóa bảng mà không nêu rõ mất dữ liệu; ưu tiên script thay đổi có kiểm soát.
- `seed.sql` chỉ dùng dữ liệu giả, nêu thứ tự chạy trong README; không dùng dữ liệu cá nhân thật.
- `application.properties` chứa cấu hình chung không nhạy cảm. `application-example.properties` là mẫu; mỗi người tạo `application-local.properties` và kích hoạt profile `local` theo README.
- Không commit mật khẩu, token, khóa bí mật hoặc cấu hình riêng. Mật khẩu tài khoản phải được băm bằng cơ chế bảo mật đã chọn; không lưu plaintext hoặc tự viết thuật toán băm.

### 10. Logging và kiểm thử

- Java dùng logger SLF4J, không dùng `System.out.println`/`printStackTrace`. TypeScript không để `console.log` debug trong bản gộp.
- Log ERROR cho lỗi ngoài dự kiến, WARN cho tình huống cần theo dõi, INFO cho mốc nghiệp vụ cần thiết, DEBUG cho chẩn đoán. Không log mật khẩu, token, session ID, chuỗi kết nối chứa bí mật hoặc toàn bộ request thanh toán.
- Log có mã đơn/giao dịch phù hợp để tra cứu; lỗi ngoài dự kiến được ghi stack trace ở nơi xử lý tập trung, tránh ghi lặp tại mọi tầng.
- Test đặt trong `src/test/java/pos/`, package tương ứng với code. Mỗi test theo Arrange → Act → Assert, kiểm tra hành vi và kết quả.
- Unit test Service không cần khởi động toàn bộ Spring; mock phụ thuộc bên ngoài khi phù hợp. Test repository/integration dùng CSDL thử riêng, không chạm dữ liệu cá nhân hoặc dữ liệu demo đang dùng.
- Bao phủ luồng đúng, đầu vào sai và trường hợp biên liên quan đến thay đổi. Với thanh toán cần kiểm tra tiền thiếu, làm tròn, tồn không đủ, ca đóng, rollback và gửi lặp; dùng integration test khi phải chứng minh transaction/ràng buộc CSDL.
- Test không phụ thuộc thứ tự chạy, thời gian thực, mạng ngoài hoặc dữ liệu còn lại từ lần chạy trước. Truyền `Clock` hoặc thời điểm cố định khi kiểm thử khuyến mãi/báo cáo.
- Postman collection lưu trong `docs/postman/`, chia theo tính năng; environment chỉ chứa giá trị mẫu. Kiểm tra cả status và nội dung trả về.
- Chạy test/build liên quan trước PR; ghi đúng lệnh và kết quả. Nếu cấu hình chưa hoàn thiện hoặc chưa chạy được, ghi rõ, không đánh dấu đạt.

### 11. Git, commit và Pull Request

| Nhánh | Vai trò |
|---|---|
| `main` | Bản ổn định để nộp/demo; gộp từ `develop` qua PR cuối sprint |
| `develop` | Nhánh tích hợp; chỉ nhận thay đổi qua PR |
| `feature/...`, `fix/...`, `docs/...`, `test/...` | Nhánh công việc tạo từ `develop` |

- Không push trực tiếp vào `main`/`develop`. Mỗi nhánh và PR tập trung vào một việc; xóa nhánh sau khi gộp.
- Tên nhánh tiếng Anh, chữ thường, kebab-case, ví dụ `feature/product-crud`, `fix/invoice-total-wrong`.
- Commit theo `type: mô tả ngắn`; mô tả dùng tiếng Anh, thể mệnh lệnh. Các type: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.
- Ví dụ: `feat: add product search`, `fix: reject checkout for closed shift`. Commit nhỏ, không đưa code đang lỗi build vào nhánh tích hợp.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/product-crud
## Viết code, kiểm tra và commit
git fetch origin
git merge origin/develop
## Xử lý conflict, kiểm tra lại rồi push
git push -u origin feature/product-crud
## Tạo PR: feature/product-crud -> develop
```

- PR ghi mã Jira/User Story, nội dung thay đổi, cách kiểm thử và kết quả; kèm ảnh nếu đổi giao diện và hướng dẫn nếu đổi schema/cấu hình.
- Gắn ít nhất một người review; không tự duyệt PR của mình. Người review phản hồi trong một ngày làm việc.
- Sửa góp ý trên cùng nhánh. Người review kiểm tra chức năng, convention, test và tác động tích hợp trước khi duyệt.
- Phân công cụ thể theo Jira; người sửa module mời người phụ trách module review. Các file dùng chung như `pom.xml`, schema, security config và layout phải nêu rõ tác động trong PR và báo nhóm trước khi tích hợp.
- Nếu cần trao đổi ngoài PR, người thực hiện chủ động thông báo trong kênh nhóm. Không ghi người phụ trách cố định trong convention khi phân công có thể thay đổi theo sprint.

### 12. Checklist trước khi gộp PR

- [ ] Đã cập nhật `origin/develop`, xử lý conflict và kiểm tra lại.
- [ ] Code đúng story và tiêu chí nghiệm thu được chọn; không mở rộng phạm vi ngoài task.
- [ ] Đúng format, tên và trách nhiệm từng lớp; Controller không gọi Repository trực tiếp.
- [ ] Validation, phân quyền và lỗi được xử lý ở máy chủ.
- [ ] Tiền, snapshot, transaction và chống gửi lặp được kiểm tra nếu thay đổi luồng giao dịch.
- [ ] Build/test liên quan đạt; ghi lệnh và kết quả trong PR.
- [ ] Không còn log debug, code chết, secret hoặc file sinh tự động.
- [ ] Schema, Entity, ERD, seed và hướng dẫn cập nhật khớp nhau nếu thay đổi dữ liệu.
- [ ] README/cấu hình mẫu/collection Postman được cập nhật nếu thay đổi ảnh hưởng cách chạy hoặc API.
- [ ] Có ít nhất một người khác review và các góp ý cần sửa đã được giải quyết.

### 13. File bỏ qua và xử lý sự cố

`.gitignore` tối thiểu:

```gitignore
target/
node_modules/
.idea/
*.iml
.vscode/
src/main/resources/static/js/
src/main/resources/application-local.properties
.env
.env.*
!.env.example
*.log
```

Không bỏ qua `package-lock.json` hoặc file mẫu cấu hình không chứa bí mật.

Khi conflict, đọc thay đổi hai bên và trao đổi với người phụ trách nếu chưa rõ; không chọn toàn bộ một phía chỉ để hết lỗi. Nếu lộ secret, báo nhóm, thu hồi/đổi secret và xử lý lịch sử theo hướng dẫn người phụ trách repo; xóa ở commit mới không làm secret cũ biến mất.

## 8. Dấu vân tay nguồn để cập nhật

SHA-256 dùng để phát hiện nguồn thay đổi; không chứng minh nội dung đã được nhóm phê duyệt hoặc kiểm thử. Không dùng đồng hồ máy để suy ra phiên bản tài liệu.

| Nguồn | SHA-256 |
|---|---|
| `POS-Supermarket/CONTRIBUTING.md` | `acd74f8e87ca7a0aa5615b5e801aed6f6cc86f910842282cf9ce8f537d675444` |
| `SyncDrive/Final DB Design/erd.mwb` | `0303aa602b9c4ac968a2021e4a94173e1f205c0c5293fb0e4c285134d8a2cc46` |
| `SyncDrive/Final DB Design/CSDL Chung-Analysis.pdf` | `591f47ef6c34fe700a75d2965e8452a1d8262c56a24a48d60e24bda53b0fdd0e` |

Khi một dấu vân tay thay đổi, đọc lại nguồn, trích lại cấu trúc và cập nhật phần hướng dẫn có liên quan. Giữ một phiên bản file này được nhóm chia sẻ; tránh để mỗi thành viên tự sửa thành quy ước riêng.
