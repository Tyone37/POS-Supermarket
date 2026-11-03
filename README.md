# POS Supermarket

Nền kỹ thuật Java/Spring Boot/Maven, Thymeleaf và TypeScript. Bootstrap hiện chưa có giao diện nghiệp vụ, đăng nhập,
Entity hoặc kết nối MySQL. Mọi request HTTP bị từ chối với mã **403** cho đến khi cơ chế bảo mật chung được tích hợp.
Không có tài khoản/mật khẩu mặc định; CSRF vẫn bật.

## Công cụ và phiên bản

| Thành phần | Phiên bản |
|---|---|
| Java target | 21; đã kiểm tra bằng Oracle JDK 25.0.2 |
| Spring Boot | 4.1.2, pin trong `pom.xml` |
| Maven | Đã kiểm tra bằng 3.9.11 |
| Node.js / npm | Đã kiểm tra bằng 24.13.0 / 11.6.2 |
| TypeScript | 7.0.2, pin trong manifest và lockfile |
| MySQL | Chưa kết nối/chạy thử; phiên bản runtime và schema chờ nhóm duyệt |

Các phiên bản bootstrap cần được nhóm review trước khi tích hợp. Bootstrap CSS/JS sẽ được thêm khi có view cần dùng;
stack giao diện vẫn là Thymeleaf/Bootstrap/TypeScript. Không cài bundler hoặc thư viện nghiệp vụ trong bước này.

## Build và chạy trên Windows

Cần JDK 21 trở lên, Maven và Node.js/npm. Đặt `JAVA_HOME` tới JDK và thêm thư mục `bin` của Maven/Node.js vào PATH.
Từ thư mục repo, chạy PowerShell:

```powershell
npm.cmd ci
npm.cmd run build
mvn.cmd -B verify -DskipTests=false -Dmaven.test.skip=false -DfailIfNoTests=true
mvn.cmd spring-boot:run
```

Dùng `npm.cmd` để tránh một shim `npm.ps1` hỏng ở môi trường Windows hiện tại. Không cần thay đổi cấu hình toàn máy;
có thể đặt biến môi trường trong riêng phiên PowerShell. Máy đã dùng để kiểm tra có Maven sẵn trong IntelliJ:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
$env:PATH = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin;C:\Program Files\nodejs;' + $env:PATH
```

Ứng dụng mặc định nghe cổng 8080. Nhận 403 khi mở trình duyệt là hành vi bootstrap đã chọn, chưa phải màn hình POS.
Nhấn Ctrl+C để dừng. Trên Linux/macOS, dùng `npm`/`mvn` thay cho tên có đuôi `.cmd`.

TypeScript bật strict và chỉ phát sinh JS khi không có lỗi. Nguồn ở `src/main/ts/`, đầu ra ở
`src/main/resources/static/js/` đã được gitignore; không sửa/commit JS sinh ra. `main.ts` hiện chỉ là module entrypoint,
chưa có hành vi UI. Chạy `npm.cmd run watch` khi làm frontend; build frontend trước Maven để JAR chứa JS mới nhất.

## Cấu hình local và dữ liệu

`application.properties` chứa cấu hình chung không nhạy cảm. Khi cần profile local:

```powershell
Copy-Item src/main/resources/application-example.properties src/main/resources/application-local.properties
mvn.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

File local đã được gitignore. Mẫu chỉ tham chiếu `POS_DB_URL`, `POS_DB_USER`, `POS_DB_PASSWORD`, không chứa credential
mặc định. Chỉ cung cấp chúng khi task tích hợp MySQL đã được duyệt; bootstrap chưa có JDBC/JPA nên không truy cập DB.

Không chạy `database/schema.sql` hoặc `database/seed.sql`: hai file vẫn rỗng, chờ schema chung được review.
SQL init bị tắt. Cấu hình `ddl-auto=validate`, `open-in-view=false` và Hibernate JDBC timezone UTC được chuẩn bị theo
convention; chúng chỉ có tác dụng sau khi tích hợp JPA. Không dùng H2 hoặc Hibernate tự tạo/sửa bảng để thay bước duyệt.

## Kiểm chứng và giới hạn

`BootstrapSmokeTest` khởi động server thật ở cổng ngẫu nhiên, không dùng DB hay mock server. Test kiểm tra
`GET /products`, `GET /login` và `POST /products` không có CSRF đều bị từ chối 403. Đây là smoke test của bootstrap,
chưa chứng minh auth/role/CSRF đầy đủ hoặc AC tạo sản phẩm của SCRUM-28.

Verifier ngoài repo được gọi từ workspace cha:

```powershell
pwsh -NoProfile -File ./POS-Harness/scripts/verify.ps1 -ProjectRoot ./POS-Supermarket
```

Verifier đầy đủ vẫn dừng với **exit 2** vì schema rỗng. Kết quả Maven/TypeScript riêng không thay thế điều kiện này
hoặc kiểm thử nghiệp vụ/tích hợp MySQL. Không thêm SQL giả hoặc bỏ preflight để báo ứng dụng đã sẵn sàng.
