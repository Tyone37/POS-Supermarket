# POS Supermarket

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
