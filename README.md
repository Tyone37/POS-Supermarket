# POS Supermarket

SCRUM-28: thêm sản phẩm, báo lỗi tại từng trường và hiển thị danh sách phân trang bằng Spring MVC/Thymeleaf.
Route `/products`, `/products/new` yêu cầu authority `PRODUCT_MANAGE`. Chưa có đăng nhập/tài khoản mặc định;
chờ tích hợp auth của Toàn. CSRF bật. Khi chưa có Authentication thật, truy cập màn hình nghiệp vụ trả **403**.

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
Repo hiện không chứa test tự động hoặc dependency JUnit/Mockito. Tài liệu ca kiểm thử được bàn giao riêng
trong thư mục `Báo cáo` của workspace cha; kết quả thực hiện để người phụ trách kiểm thử ghi nhận.
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
Code chỉ ánh xạ sản phẩm/category/unit/movement; các bảng khác trong DDL phục vụ FK, không đồng nghĩa đã có checkout.
Luồng quét SCRUM-35 phải dùng cùng quy tắc barcode; AC28-4 chờ tích hợp. Tài liệu ca thử không chứng minh race/rollback MySQL,
phiên đăng nhập thật hoặc mức khớp giao diện trên trình duyệt.

Verifier ngoài repo, chạy từ workspace cha:

```powershell
pwsh -NoProfile -File ./POS-Harness/scripts/verify.ps1 -ProjectRoot ./POS-Supermarket
```

Verifier dùng `failIfNoTests=true`; khi người phụ trách chưa thêm bộ test, verifier sẽ báo thiếu test.
Không sửa verifier chung để bỏ điều kiện này; dùng lệnh build ở trên cho phần việc viết code hiện tại.
