# POS Supermarket

## Bản hiện tại

Spring Boot 3.5.16, Java 21, Spring Security, Thymeleaf, TypeScript và MySQL.
Dữ liệu được đọc/ghi qua JPA: các lớp `*Entity`, EntityManager và JPQL.
Production code không dùng JdbcClient, JdbcTemplate hoặc SQL native.

- `AccountRepository` đọc account/role/permission bằng JPA và trả Account DTO cho Spring Security.
- `PosRepository` quản lý entity, truy vấn JPQL và khóa PESSIMISTIC_WRITE; chuyển dữ liệu thành map cho service/view hiện hành.
- Service giữ validation, quyền, tiền/tồn, transaction và retry. Các route/template hiện tại được giữ.
- FK ánh xạ bằng ID trong entity để giữ hợp đồng service. Generated columns của MySQL không được ghi qua JPA.
- Entity ca chỉ ánh xạ phần xem/mở ca; dữ liệu đối soát đóng ca vẫn nằm trong DB, chưa có chức năng ghi đóng ca.

## Database và cấu hình

Dùng MySQL >= 8.0.16. Với DB rỗng, áp `database/schema.sql` rồi `database/seed.sql`.
Với DB cũ, rà soát/sao lưu rồi áp `database/migrations/001-sprint-1.sql` theo hướng dẫn trong file; không chạy lại schema tạo bảng.
Seed bổ sung danh mục mặc định UNCLASSIFIED/CAI. `database/dev-sample-data.sql` là dữ liệu demo riêng, không phải migration và không dùng cho dữ liệu thật.

JPA dùng `ddl-auto=validate`, `open-in-view=false`, JDBC timezone UTC. Không tự tạo/xóa/sửa schema hoặc tự chạy SQL init.
DB thiếu cột sẽ khiến app không khởi động; chuyển sang JPA không thay thế cập nhật migration.

Cấu hình chính nhận POS_DB_URL, POS_DB_USERNAME và POS_DB_PASSWORD qua môi trường, có fallback phát triển trong file cấu hình.
Profile local hiện có dùng POS_DB_USER cho username; kiểm tra đúng profile và biến môi trường của máy.
Giữ mật khẩu trong môi trường hoặc application-local.properties đã gitignore; không commit credential.
Thời điểm DATETIME(3) được xử lý theo UTC. Tiền dùng BigDecimal; dữ liệu sản phẩm/giá/khuyến mãi lịch sử dùng snapshot.

## Build và chạy trên PowerShell

Cần JDK 21+, Node.js/npm, Maven và node_modules đã cài theo package-lock.json.
Maven tự biên dịch TypeScript ở generate-resources.

```powershell
npm.cmd ci
mvn.cmd -B verify
if ($LASTEXITCODE -eq 0) {
    java -jar target/pos-supermarket-0.0.1-SNAPSHOT.jar
}
```

Dừng app đang chạy trước khi thay JAR. Thay properties trong source cần build lại khi chạy bằng JAR.
Mở http://localhost:8080/login. Đăng xuất POST với CSRF; tài khoản ACTIVE và BCrypt được kiểm tra ở máy chủ.
Các trang workspace điều hướng tới nghiệp vụ theo role.

| Vai trò | Màn hình |
|---|---|
| CASHIER | /shifts, /sales và hóa đơn do mình phát hành |
| MANAGER | /products và xem hóa đơn |
| ADMIN | /accounts |

Phạm vi hiện tại có: tạo sản phẩm/ảnh HTTPS, xem/mở ca, tạo DRAFT theo request key, quét/tìm/gộp hàng,
đổi lượng/xóa có xác nhận, hủy với lý do, checkout CASH/BANK_TRANSFER, invoice snapshot và xem/in.
Chuyển khoản cần cấu hình tài khoản nhận và xác nhận thực tế theo hợp đồng hiện hành; không giả xác nhận nhận tiền.
Dòng có promotion/discount sẵn không đổi lượng khi chưa có engine tính lại; vẫn có thể xóa.
Chưa có chức năng đóng ca, engine khuyến mãi hoặc hóa đơn điện tử.

## Kiểm chứng và bàn giao

Unit test dùng JUnit 5 và Mockito qua `spring-boot-starter-test`, đặt trong `src/test/java/pos/service`.
Bộ test cơ bản, mỗi hàm kiểm tra một tình huống, tên hàm bằng tiếng Việt không dấu để dễ đọc.
Không dùng test tham số hóa hoặc helper dữ liệu chung. Mock repository để không cần MySQL và không sửa dữ liệu ứng dụng.
Riêng `LoginTest` dùng AuthenticationProvider thật của ứng dụng cùng BCrypt để kiểm tra đúng/sai mật khẩu;
không chỉ kiểm tra hàm tải thông tin tài khoản.

| File | Tình huống cơ bản |
|---|---|
| `LoginTest` | Đúng tài khoản/mật khẩu; sai mật khẩu; tên không tồn tại; bỏ trống tên/mật khẩu; tài khoản khóa |
| `AccountAdminServiceTest` | Tạo hợp lệ; trùng tên; mật khẩu ngắn; không được tự khóa |
| `CatalogServiceTest` | Thêm hợp lệ; giá âm; tồn âm; tìm tên không dấu |
| `ShiftServiceTest` | Mở ca hợp lệ; tiền âm; chưa chọn quầy; chưa có ca |
| `SaleServiceTest` | Tiền mặt hợp lệ/tính thừa; thiếu tiền; chuyển khoản chưa xác nhận; mã sản phẩm sai; số lượng âm; xác nhận/hủy bỏ hủy đơn |


```powershell
mvn.cmd -B test
# Chạy test và đóng gói, giống CI:
mvn.cmd -B verify
# Chỉ chạy một nhóm:
mvn.cmd -B "-Dtest=SaleServiceTest" test
```

Kết quả chi tiết nằm trong `target/surefire-reports`. CI chạy `mvn -B verify`, không bỏ qua test.
Unit test với mock không chứng minh transaction rollback, khóa/unique constraint hoặc tính đúng khi nhiều quầy gửi đồng thời.
Những phần đó cần integration test trên MySQL riêng và kiểm thử giao diện/nghiệm thu AC.

Bản JPA dùng JAR mặc định `target/pos-supermarket-0.0.1-SNAPSHOT.jar` và cổng 8080.
Cổng 8081 và thư mục `target/jpa-build/` chỉ dùng trong lượt kiểm tra tách biệt trước khi thay runtime cũ.
Đã dùng thử thủ công các luồng chính trên CSDL thử riêng; kết quả không thay nghiệm thu toàn bộ AC.
