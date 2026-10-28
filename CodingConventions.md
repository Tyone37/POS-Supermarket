# Coding conventions và quy ước đóng góp

Tài liệu này thống nhất cách viết code và phối hợp trong dự án POS Supermarket. Mọi thành viên áp dụng khi viết mới hoặc sửa code. Thay đổi quy ước bằng Pull Request (PR) để nhóm review.

## 1. Công nghệ và cấu trúc dự án

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

## 2. Định dạng và cách trình bày code

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

## 3. Quy tắc đặt tên

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

## 4. Trách nhiệm từng lớp và Java/Spring

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

## 5. Validation, phân quyền và xử lý lỗi

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

## 6. Tiền tệ, thời gian và transaction

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

## 7. Controller, API và truy vấn

- Trang Thymeleaf dùng `@Controller`; API JSON dùng `@RestController`, tên `XxxApiController`, prefix `/api/`.
- API dùng danh từ số nhiều: `GET /api/products`, `POST /api/products`, `PUT /api/products/{id}`. Thao tác nghiệp vụ dùng đường dẫn rõ như `POST /api/orders/{id}/checkout`.
- GET chỉ đọc; không dùng GET để xóa, thanh toán hoặc thay đổi trạng thái.
- Tạo mới trả 201; đọc/cập nhật có dữ liệu trả 200; thành công không có body trả 204. Không trả HTTP 200 cho lỗi.
- Form ghi dữ liệu thành công dùng redirect để tránh gửi lại khi refresh; chống thanh toán lặp vẫn phải thực hiện ở Service/CSDL.
- Danh sách có thể tăng lớn phải phân trang; giới hạn kích thước trang và cho phép sắp xếp trên danh sách trường được quy định.
- Tránh truy vấn trong vòng lặp gây N+1; lấy đủ dữ liệu bằng query/projection phù hợp. Chỉ thêm index dựa trên nhu cầu truy vấn cụ thể.

## 8. TypeScript, Thymeleaf và CSS

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

## 9. CSDL và cấu hình

- `database/schema.sql` là nguồn chuẩn cấu trúc bảng. ERD, Entity và tài liệu dữ liệu phải khớp schema sau mỗi thay đổi.
- Chốt tên bảng/khóa và bên sở hữu dữ liệu trước khi tích hợp. Không đồng thời tạo `sales_items` và `order_items` cho cùng một loại dữ liệu chỉ vì hai tài liệu dùng tên khác nhau.
- Khóa ngoại phải tương thích kiểu với khóa được tham chiếu, kể cả signed/unsigned. Khai báo rõ NOT NULL, UNIQUE, FK và CHECK phù hợp; kiểm tra nghiệp vụ nhiều dòng vẫn thuộc Service/transaction.
- Tài liệu thiết kế logic phải được chuyển sang DDL MySQL hợp lệ trước khi đưa vào schema.
- Dùng `spring.jpa.hibernate.ddl-auto=validate`; không dùng Hibernate tự sửa schema chung.
- Đổi schema bằng PR nhỏ, kèm hướng dẫn cập nhật dữ liệu hiện có và dữ liệu mẫu. Không yêu cầu cả nhóm chạy lại script có lệnh xóa bảng mà không nêu rõ mất dữ liệu; ưu tiên script thay đổi có kiểm soát.
- `seed.sql` chỉ dùng dữ liệu giả, nêu thứ tự chạy trong README; không dùng dữ liệu cá nhân thật.
- `application.properties` chứa cấu hình chung không nhạy cảm. `application-example.properties` là mẫu; mỗi người tạo `application-local.properties` và kích hoạt profile `local` theo README.
- Không commit mật khẩu, token, khóa bí mật hoặc cấu hình riêng. Mật khẩu tài khoản phải được băm bằng cơ chế bảo mật đã chọn; không lưu plaintext hoặc tự viết thuật toán băm.

## 10. Logging và kiểm thử

- Java dùng logger SLF4J, không dùng `System.out.println`/`printStackTrace`. TypeScript không để `console.log` debug trong bản gộp.
- Log ERROR cho lỗi ngoài dự kiến, WARN cho tình huống cần theo dõi, INFO cho mốc nghiệp vụ cần thiết, DEBUG cho chẩn đoán. Không log mật khẩu, token, session ID, chuỗi kết nối chứa bí mật hoặc toàn bộ request thanh toán.
- Log có mã đơn/giao dịch phù hợp để tra cứu; lỗi ngoài dự kiến được ghi stack trace ở nơi xử lý tập trung, tránh ghi lặp tại mọi tầng.
- Test đặt trong `src/test/java/pos/`, package tương ứng với code. Mỗi test theo Arrange → Act → Assert, kiểm tra hành vi và kết quả.
- Unit test Service không cần khởi động toàn bộ Spring; mock phụ thuộc bên ngoài khi phù hợp. Test repository/integration dùng CSDL thử riêng, không chạm dữ liệu cá nhân hoặc dữ liệu demo đang dùng.
- Bao phủ luồng đúng, đầu vào sai và trường hợp biên liên quan đến thay đổi. Với thanh toán cần kiểm tra tiền thiếu, làm tròn, tồn không đủ, ca đóng, rollback và gửi lặp; dùng integration test khi phải chứng minh transaction/ràng buộc CSDL.
- Test không phụ thuộc thứ tự chạy, thời gian thực, mạng ngoài hoặc dữ liệu còn lại từ lần chạy trước. Truyền `Clock` hoặc thời điểm cố định khi kiểm thử khuyến mãi/báo cáo.
- Postman collection lưu trong `docs/postman/`, chia theo tính năng; environment chỉ chứa giá trị mẫu. Kiểm tra cả status và nội dung trả về.
- Chạy test/build liên quan trước PR; ghi đúng lệnh và kết quả. Nếu cấu hình chưa hoàn thiện hoặc chưa chạy được, ghi rõ, không đánh dấu đạt.

## 11. Git, commit và Pull Request

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
# Viết code, kiểm tra và commit
git fetch origin
git merge origin/develop
# Xử lý conflict, kiểm tra lại rồi push
git push -u origin feature/product-crud
# Tạo PR: feature/product-crud -> develop
```

- PR ghi mã Jira/User Story, nội dung thay đổi, cách kiểm thử và kết quả; kèm ảnh nếu đổi giao diện và hướng dẫn nếu đổi schema/cấu hình.
- Gắn ít nhất một người review; không tự duyệt PR của mình. Người review phản hồi trong một ngày làm việc.
- Sửa góp ý trên cùng nhánh. Người review kiểm tra chức năng, convention, test và tác động tích hợp trước khi duyệt.
- Phân công cụ thể theo Jira; người sửa module mời người phụ trách module review. Các file dùng chung như `pom.xml`, schema, security config và layout phải nêu rõ tác động trong PR và báo nhóm trước khi tích hợp.
- Nếu cần trao đổi ngoài PR, người thực hiện chủ động thông báo trong kênh nhóm. Không ghi người phụ trách cố định trong convention khi phân công có thể thay đổi theo sprint.

## 12. Checklist trước khi gộp PR

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

## 13. File bỏ qua và xử lý sự cố

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
