# POS Supermarket

## Bản triển khai hiện tại

Nhánh `feature/scrum-24-login`: Spring Boot 3.5.16, Java 21, Spring Security,
Thymeleaf, Validation, Spring JDBC và MySQL. JDBC dùng SQL có tham số, tách
Controller → Service → Repository. Chưa sử dụng JPA nên không có Entity JPA
hoặc cơ chế tự tạo/sửa schema. `Account` là bản ghi dữ liệu đọc từ schema chung.

## Chuẩn bị CSDL

1. Cài MySQL >= 8.0.16, tạo CSDL thử rỗng `pos_supermarket` bằng công cụ của bạn.
2. Chạy `database/schema.sql` một lần trên CSDL thử này. File được sao nguyên
   từ schema người dùng cung cấp; tiêu đề còn ghi bản đề xuất chờ nhóm review.
   Ứng dụng không tự chạy schema và không tự xóa dữ liệu.
3. Tạo role với code `CASHIER`, `MANAGER`, `ADMIN` theo vai trò cần kiểm tra.
   Đây là các mã ứng dụng đề xuất, cần nhóm xác nhận khi tích hợp.
4. Thêm tài khoản vào `accounts` với `role_id` tương ứng, `status=ACTIVE`,
   `created_at` và `updated_at` theo UTC. `password_hash` phải là BCrypt,
   không lưu mật khẩu gốc. Dùng BCryptPasswordEncoder để tạo hash khi chuẩn bị dữ liệu thử.
5. Sao chép `src/main/resources/application-example.properties` thành
   `application-local.properties`, điền thông tin MySQL của máy bạn.

Chưa có tài khoản/mật khẩu mặc định; không tạo tài khoản thật trong seed.

## Chạy

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Trong PowerShell, nếu cần, đặt tham số profile trong dấu nháy:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Mở http://localhost:8080/login. Đúng thông tin sẽ chuyển đến trang đích theo
vai trò. Các trang `/workspace/*` chỉ là trang xác nhận đăng nhập, chưa phải
màn bán hàng/quản lý của Tú. Sai mật khẩu/tài khoản khóa hiện lỗi tiếng Việt.
Đăng xuất dùng POST với CSRF; không bật JWT hoặc remember-me.

Chỉ dùng ở máy phát triển. Khi triển khai HTTPS phải bật secure session cookie.
Không commit file cấu hình local hoặc mật khẩu.

## Kiểm thử

```bash
mvn -B verify
```

MockMvc kiểm tra toàn bộ filter Spring Security, form đăng nhập, BCrypt,
điều hướng, CSRF, chặn quyền trang đích và đăng xuất. Repository được mock;
không coi kết quả này là kiểm thử SQL/MySQL. Chưa xác minh repository/schema
trên MySQL thật vì môi trường hiện chưa chạy MySQL/Docker.

## Phần tiếp theo

- SCRUM-25: kiểm tra đầy đủ hành vi quay lại/tải lại sau đăng xuất.
- SCRUM-26: chốt ma trận permissions và áp dụng cho endpoint nghiệp vụ.
- SCRUM-27: tạo/khóa tài khoản, xử lý phiên khi tài khoản bị khóa.
- Lưu/thu hồi `login_sessions` theo hợp đồng DB: chưa triển khai ở nhánh này;
  hiện Spring Security dùng HttpSession với timeout 30 phút không hoạt động.
- SCRUM-39/42: thanh toán sau khi ghép đơn hàng, ca và hóa đơn của Tú.
- Frontend TypeScript/Bootstrap và CI chung vẫn cần tích hợp; chưa có lockfile
  nên chưa chạy npm. Không merge nhánh này thay cho PR thiết lập CI đang mở.
Khung Spring Boot cho Sprint 1, dùng Java 21 và Spring Boot 3.5.16.

## Yêu cầu

- JDK 21.
- Maven 3.6.3 trở lên.
- Đã kiểm tra trên Windows với Temurin 21.0.12.1 và Maven 3.9.16.

## Chạy ứng dụng

```bash
mvn spring-boot:run
```

Ứng dụng chạy tại http://localhost:8080. Hiện chưa có màn hình nghiệp vụ,
nên truy cập `/` trả 404 là bình thường. Dừng bằng Ctrl+C.

## Build và kiểm thử

```bash
mvn -B verify
```

Lệnh chạy kiểm thử khởi động Spring và tạo file
`target/pos-supermarket-0.0.1-SNAPSHOT.jar`.

```bash
java -jar target/pos-supermarket-0.0.1-SNAPSHOT.jar
```

## Phạm vi khởi tạo

Đã có Spring MVC, Thymeleaf, Validation và JUnit/Spring Boot Test.
Chưa có đăng nhập, phân quyền, nghiệp vụ hoặc kết nối CSDL.
Chỉ chạy bản nền này tại máy phát triển; chưa triển khai công khai.

MySQL và Spring Data JPA sẽ được cấu hình khi tích hợp schema chung đã review.
Không dùng H2 thay MySQL để coi như đã kiểm thử CSDL. Cấu hình
`ddl-auto=validate` đã được chuẩn bị cho bước tích hợp JPA.
TypeScript, Bootstrap và CI frontend chưa được khởi tạo trong bước này;
Node.js và MySQL chưa được dùng để kiểm thử bản nền.

CI hiện chạy `mvn -B verify` khi có PR vào `develop`/`main` và khi push vào `develop`.
