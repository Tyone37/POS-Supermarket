# POS Supermarket

Phần Sprint 1 của Tú: sản phẩm, xem/mở ca, giỏ hàng, hủy đơn nháp và hóa đơn bằng Spring MVC/Thymeleaf.
Đã triển khai code và build theo phạm vi được duyệt; chưa nghiệm thu AC. Người khác phụ trách toàn bộ kiểm thử.
Chưa có đăng nhập/tài khoản mặc định hoặc checkout/payment; chờ tích hợp của Toàn. CSRF bật.
Khi chưa có Authentication thật, truy cập màn hình nghiệp vụ trả **403**.

## Công cụ và phiên bản

| Thành phần | Phiên bản |
|---|---|
| Java target | 21; máy phát triển dùng Oracle JDK 25.0.2 |
| Spring Boot | 4.1.2 |
| Maven | 3.9.11 |
| Node.js / npm | 24.13.0 / 11.6.2 |
| TypeScript | 7.0.2 |
| Bootstrap CSS | 5.3.8 qua WebJar, phục vụ cục bộ |
| MySQL | DDL S đã thử cục bộ bằng MySQL Community 9.7; môi trường chung chờ nhóm xác nhận |

Không dùng React, Tailwind, bundler hoặc CDN. Các phiên bản/dependency cần nhóm review trước khi tích hợp.

## Build

Từ thư mục repo, với JDK 21+, Maven và Node.js/npm trên PATH:

```powershell
npm.cmd ci
npm.cmd run build
mvn.cmd -B clean package -Dmaven.test.skip=true
```

Theo phân công của người dùng, người khác phụ trách viết và chạy kiểm thử, bao gồm unit test.
Repo hiện không chứa test tự động hoặc dependency JUnit/Mockito. Thiết kế unit test SCRUM-28 được bàn giao riêng
trong thư mục `Báo cáo` của workspace cha; chưa có tài liệu unit test cho các story còn lại.
Các lệnh trên chỉ biên dịch và đóng gói, không nghiệm thu chức năng.

Dùng `npm.cmd` để tránh shim PowerShell hỏng trong môi trường Windows hiện tại. Có thể cấu hình toolchain cho riêng
phiên PowerShell, không cần sửa cấu hình toàn máy:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
$env:PATH = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin;C:\Program Files\nodejs;' + $env:PATH
```

Nguồn TypeScript nằm trong `src/main/ts/`; JS sinh ở `src/main/resources/static/js/` đã được gitignore.
Chạy `npm.cmd run build` **trước Maven package/verify** để JAR chứa JS hiện hành; không sửa/commit JS sinh ra.
Chạy `npm.cmd run watch` khi làm frontend. Linux/macOS dùng `npm`/`mvn` thay đuôi `.cmd`.

## Cấu hình chạy cục bộ

Người phụ trách môi trường chuẩn bị CSDL MySQL riêng và áp dụng `database/schema.sql`, sau đó `database/seed.sql`
một lần trên CSDL mới. Không áp các file này lên dữ liệu đang dùng. SQL init tự động bị tắt;
Hibernate chỉ `validate` schema, không tự tạo/sửa bảng; `open-in-view=false`, JDBC timezone UTC.

```powershell
Copy-Item src/main/resources/application-example.properties src/main/resources/application-local.properties
# Cung cấp POS_DB_URL, POS_DB_USER, POS_DB_PASSWORD qua môi trường riêng; không commit credential.
# URL ví dụ: jdbc:mysql://localhost:3306/pos_local?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
npm.cmd run build
mvn.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

File local đã gitignore. App nghe cổng 8080; Ctrl+C để dừng. Cần tích hợp Authentication thật cấp `PRODUCT_MANAGE`
trước khi dùng form. `BootstrapSecurityConfig` hiện không chứa tài khoản; người tích hợp auth thay cấu hình này
bằng hợp đồng chung, giữ quyền route/service và CSRF.

## Hợp đồng tạo sản phẩm

- Barcode là chuỗi, bỏ khoảng trắng hai đầu, giữ số 0 đầu và nội dung còn lại; tối đa 50 ký tự.
- Tên bắt buộc, bỏ khoảng trắng hai đầu, tối đa 200 ký tự. Giá > 0 theo DECIMAL(18,2), tồn >= 0 theo DECIMAL(15,3).
  Số thập phân nhập bằng dấu chấm. Không tự làm tròn lượng; phải khớp scale 0 hoặc 3 của đơn vị.
- Category trống dùng code `UNCLASSIFIED` ACTIVE; unit trống dùng `CAI` scale 0; seed cung cấp hai bản ghi này.
  Nếu người dùng chọn, category phải ACTIVE và unit phải tồn tại; không dùng ID cố định.
- Server sinh SKU `SP` + 18 ký tự hex viết hoa, trạng thái ACTIVE và reserved 0. Không nhận các trường này từ form.
- UNIQUE barcode/SKU trong MySQL phân xử trùng, kể cả ghi đồng thời. Trùng barcode báo tại ô mã vạch;
  trùng SKU yêu cầu thử lại; lỗi SQL khác được truyền ra, không báo lưu thành công.
- Tồn > 0 tạo ADJUSTMENT với `PRODUCT_CREATE:<SKU>` trong cùng transaction; tồn 0 không ghi movement.
- Danh sách 20 hàng/trang, mới nhất trước theo createdAt/id; giá giữ hai chữ số thập phân.

DDL là bản S người dùng cung cấp và duyệt dùng cho triển khai, chưa thay thế review schema chung của nhóm.
Code đã ánh xạ các bảng cần cho phạm vi Tú; việc có mapping order/payment không đồng nghĩa đã có checkout.
Luồng quét SCRUM-35 dùng cùng quy tắc barcode; AC28-4 chờ kiểm thử luồng thực tế. Thiết kế unit test không chứng minh race/rollback MySQL,
phiên đăng nhập thật hoặc mức khớp giao diện trên trình duyệt.

Verifier ngoài repo, chạy từ workspace cha:

```powershell
pwsh -NoProfile -File ./POS-Harness/scripts/verify.ps1 -ProjectRoot ./POS-Supermarket
```

Verifier dùng `failIfNoTests=true`; khi người phụ trách chưa thêm bộ test, verifier sẽ báo thiếu test.
Không sửa verifier chung để bỏ điều kiện này; dùng lệnh build ở trên cho phần việc viết code hiện tại.

## Ca làm việc và giỏ hàng

| Route hoặc service | Quyền | Hành vi |
|---|---|---|
| `/products`, `/products/new` | PRODUCT_MANAGE | Tạo và danh sách sản phẩm |
| GET `/shifts/current` | SHIFT_VIEW | OPEN của chính nhân viên, nếu không thì ca CLOSED gần nhất hoặc chưa mở |
| GET `/shifts/new`, POST `/shifts` | SHIFT_OPEN | Chọn quầy ACTIVE, tiền >= 0; khóa employee, UUID key so payload để xử lý retry |
| `/sale`, `/sale/**` | SALE_USE | Ca OPEN của actor; tạo DRAFT bằng POST, quét/tìm/thêm dùng chung logic |
| `/invoices`, `/invoices/**` | INVOICE_VIEW | Tra đúng mã hoặc đọc theo ID; chỉ hóa đơn do account/employee của mình phát hành |
| `InvoiceService.issue(paymentId)` | INVOICE_ISSUE | Hàm nội bộ trong transaction checkout có sẵn; không có endpoint phát hành riêng |

Authentication.name phải khớp accounts.username; account và employee liên kết phải ACTIVE.
Người dùng mở ca cần cả SHIFT_OPEN và SHIFT_VIEW để xem trang kết quả; quyền/session do module auth cấp.
Không tạo tài khoản hoặc danh mục quầy giả để demo. Danh mục quầy lấy từ bảng registers đã được nhóm chuẩn bị.

Giỏ chỉ sửa/hủy DRAFT thuộc ca OPEN của nhân viên. PENDING/PAID/CANCELLED không được sửa; hủy lặp cùng đơn đã hủy
không tạo thay đổi lần nữa. Ghi theo thứ tự khóa ca → đơn → sản phẩm; module đóng ca/checkout phải dùng cùng thứ tự.
Các thao tác giỏ không giữ hoặc trừ tồn; vẫn kiểm tra stock - reserved khi thêm/đổi lượng và checkout cần kiểm tra lại.
Giá/snapshot từ server; lượng >= 1, đúng scale đơn vị 0/3; tiền làm tròn HALF_UP 2 chữ số.
Thao tác lượng 0 hoặc xóa mở xác nhận; bỏ xác nhận giữ lượng cũ. Nhãn giá/tiền chỉ định dạng khi hiển thị.

Phạm vi khuyến mãi đã duyệt: dòng mới chưa áp giảm; dòng có promotion/discount từ trước chặn đổi lượng để tránh
tính sai khi chưa có hợp đồng tính lại. Xóa dòng vẫn tính lại tổng từ các dòng còn lại. AC37-4 phần giảm giá chờ tích hợp.
Ảnh sản phẩm hiện là placeholder có nhãn; AC36-2 phần ảnh thật chờ nguồn ảnh. Hủy đơn giữ dòng, tổng và cancelled_at,
chưa có nơi lưu actor/lý do/ghi chú hủy; AC40-2 phần audit/lý do còn chờ. Ghi chú mở ca không được giả vờ lưu.

## Hợp đồng tích hợp hóa đơn với checkout

Toàn cung cấp auth và checkout SCRUM-39/42. Caller phải dùng cùng transaction JPA, thống nhất thứ tự khóa ca → đơn → payment →
sản phẩm (ID tăng dần khi cập nhật tồn), chốt snapshot/tổng, đánh dấu payment SUCCESS và order PAID,
rồi gọi `InvoiceService.issue(paymentId)` trước commit. Caller sở hữu idempotency key/payload checkout, trừ tồn và
inventory SALE; service hóa đơn không làm những thao tác đó và không mở transaction riêng.

`issue` dùng propagation MANDATORY: gọi ngoài transaction thất bại. Service kiểm tra quyền, account/employee,
sở hữu ca, SUCCESS/PAID/CASH, tiền nhận/thừa, tổng đơn và tổng snapshot từng dòng. Invoice có sẵn đúng đơn/payment/tổng
trả lại ID mà không ghi thêm; phát hành mới cần ca OPEN. Lỗi phải truyền ra để caller rollback toàn bộ.
Checkout thực tế và các method tạo/chuyển trạng thái Order/Payment của module đó chưa được triển khai ở phần Tú.

Trang hóa đơn chỉ đọc snapshot sản phẩm/giá/khuyến mãi và số tiền đã lưu, không tính lại theo danh mục hiện tại;
có thể xem lại khi ca CLOSED. Nút in chỉ in trang hiện tại, không tạo chứng từ mới. Tên nhân viên/quầy đang lấy từ
tham chiếu hiện tại vì schema không có snapshot tương ứng. Không có thuế/VAT, QR hoặc hóa đơn điện tử.
AC tự tạo hóa đơn sau thanh toán, rollback/retry checkout và in/giao diện vẫn chờ tích hợp cùng tester xác nhận.
