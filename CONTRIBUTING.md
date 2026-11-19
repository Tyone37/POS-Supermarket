# Quy ước làm việc nhóm

Tài liệu này là quy ước chung của cả nhóm. **Mọi thành viên đọc trước khi viết code.**
Muốn đổi quy ước: bàn với cả nhóm, rồi sửa file này bằng một Pull Request (PR).

---

## 1. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Giao diện | Thymeleaf, Bootstrap, TypeScript (biên dịch ra JavaScript) |
| Máy chủ | Java + Spring Boot, mô hình MVC (Controller, Service, Repository) |
| Cơ sở dữ liệu | MySQL |
| Kiểm thử | JUnit (hàm xử lý), Postman (API) |

---

## 2. Nhánh (branch)

| Nhánh | Vai trò | Ai được sửa |
|---|---|---|
| `main` | Bản ổn định, chạy được, dùng để nộp/demo | Chỉ gộp từ `develop` cuối mỗi sprint (người phụ trách repo) |
| `develop` | Nhánh tích hợp chung | Chỉ gộp qua PR |
| `feature/...`, `fix/...` | Nhánh làm việc của từng người | Người tạo nhánh |

**Quy tắc cứng:**

1. **Không push trực tiếp vào `main` và `develop`.** Mọi thay đổi đều đi qua PR.
2. **Mỗi tính năng (hoặc mỗi lỗi) một nhánh riêng**, tạo từ `develop`.
3. Một nhánh chỉ làm **một việc**, sống ngắn (vài ngày), gộp xong thì xóa.

**Đặt tên nhánh:** `loại/mô-tả-ngắn`, chữ thường, tiếng Anh, nối bằng dấu gạch ngang.

```
feature/login
feature/product-crud
feature/cart-checkout
fix/invoice-total-wrong
docs/update-erd
test/payment-service
```

**Quy trình làm một việc:**

```bash
git checkout develop
git pull origin develop                  # luôn lấy bản mới nhất trước
git checkout -b feature/product-crud     # tạo nhánh của mình

# ... code, commit nhiều lần ...

git pull origin develop                  # cập nhật nhánh mình, xử lý conflict nếu có
git push -u origin feature/product-crud  # đẩy nhánh lên
# Mở Pull Request: feature/product-crud -> develop
```

---

## 3. Commit

Định dạng: `loại: mô tả ngắn` (viết ở thể mệnh lệnh, tiếng Anh hoặc tiếng Việt đều được nhưng **thống nhất trong cả nhóm**).

| Loại | Dùng khi |
|---|---|
| `feat` | Thêm tính năng |
| `fix` | Sửa lỗi |
| `docs` | Sửa tài liệu |
| `test` | Thêm/sửa test |
| `refactor` | Sửa cấu trúc code, không đổi chức năng |
| `chore` | Việc lặt vặt (cấu hình, cập nhật thư viện) |

Ví dụ: `feat: add product search`, `fix: wrong discount calculation`, `docs: update ERD`.

Commit nhỏ, mỗi commit một ý. Không commit code đang lỗi build.

---

## 4. Pull Request (PR)

- Tiêu đề rõ ràng, theo cùng kiểu commit: `feat: product CRUD`.
- Mô tả ngắn: **làm gì, test thế nào**, kèm ảnh chụp màn hình nếu có thay đổi giao diện.
- Gắn ít nhất **1 người review** (không tự duyệt PR của mình).
- Một PR chỉ làm một việc, càng nhỏ càng dễ review.
- Người review trả lời trong vòng 1 ngày làm việc. Người viết sửa theo góp ý rồi đẩy tiếp lên cùng nhánh.

**Checklist trước khi tạo PR** (tự kiểm tra):

- [ ] Đã `git pull origin develop` vào nhánh của mình và hết conflict
- [ ] Chạy được chương trình, không lỗi build
- [ ] Test JUnit liên quan chạy qua
- [ ] Không còn code thử nghiệm, `System.out.println`, hay file thừa
- [ ] Không commit mật khẩu, file cấu hình cá nhân, `node_modules/`, `target/`
- [ ] Controller không chứa logic nghiệp vụ và không gọi thẳng Repository (xem mục 6)
- [ ] Nếu đổi cấu trúc bảng: đã cập nhật `database/schema.sql` và báo cả nhóm

**Người review kiểm tra:** đúng chức năng, đúng quy tắc mục 6 và mục 7, có test, không phá phần của người khác.

---

## 5. Cấu trúc thư mục

```
pos-supermarket/
├── README.md                    # giới thiệu, cách cài đặt và chạy
├── CONTRIBUTING.md              # quy ước của nhóm
├── .gitignore
├── pom.xml                      # (hoặc build.quy ước của nhómgradle)
├── package.json                 # chỉ để biên dịch TypeScript
├── tsconfig.json
│
├── docs/
│   ├── requirements/            # SRS, user story
│   ├── design/                  # use case, class, sequence, ERD
│   └── postman/                 # collection Postman (.json), chia folder theo tính năng
│
├── database/
│   ├── schema.sql               # cấu trúc bảng (nguồn chuẩn)
│   └── seed.sql                 # dữ liệu mẫu
│
└── src/
    ├── main/
    │   ├── java/pos/   
    │   │   ├── PosApplication.java
    │   │   ├── config/          # security, web config
    │   │   ├── controller/      # C: nhận yêu cầu từ giao diện
    │   │   ├── service/         # xử lý nghiệp vụ
    │   │   ├── repository/      # làm việc với MySQL
    │   │   ├── entity/          # M: các lớp ánh xạ bảng
    │   │   ├── dto/             # dữ liệu vào/ra của form và API
    │   │   ├── exception/
    │   │   └── util/
    │   ├── ts/                  # TypeScript nguồn, chia thư mục theo tính năng
    │   └── resources/
    │       ├── application.properties
    │       ├── application-example.properties
    │       ├── templates/       # V: Thymeleaf, chia thư mục theo tính năng
    │       │   ├── layout/      # header, sidebar, khung chung
    │       │   └── <tính-năng>/ # auth/, product/, sale/, ...
    │       └── static/
    │           ├── css/
    │           ├── img/
    │           └── js/          # JS biên dịch ra, KHÔNG commit
    └── test/java/pos/  # JUnit

## 6. Quy tắc từng lớp (MVC)

| Lớp | Việc của lớp | Không được làm |
|---|---|---|
| **View** (`templates/`, `ts/`) | Hiển thị, nhận thao tác người dùng | Chứa logic nghiệp vụ |
| **Controller** | Nhận request, kiểm tra dữ liệu đầu vào, gọi Service, chọn trang/trả kết quả | Viết logic nghiệp vụ, gọi thẳng Repository |
| **Service** | Xử lý nghiệp vụ (tính tiền, kiểm tra giỏ, lưu hóa đơn, trừ tồn kho) | Biết gì về HTML hay HTTP request |
| **Repository** | Truy vấn và lưu dữ liệu MySQL | Chứa nghiệp vụ |
| **Entity** | Ánh xạ bảng trong CSDL | Chứa logic xử lý phức tạp |

Luồng chuẩn: `View -> Controller -> Service -> Repository -> MySQL`.

- Trang HTML trả về bằng `@Controller`.
- Endpoint JSON cho TypeScript gọi dùng `@RestController`, đặt tên `XxxApiController`, đường dẫn bắt đầu bằng `/api/`.
- Dữ liệu đi giữa Controller và giao diện dùng **DTO**, không trả thẳng Entity ra ngoài.

---

## 7. Đặt tên

- Tên lớp theo mẫu `<TínhNăng><Lớp>`: `ProductController`, `ProductService`, `ProductRepository`, `Product` (entity), `ProductRequest` (DTO).
- Lớp `PascalCase`, biến và hàm `camelCase`, hằng số `UPPER_SNAKE_CASE`.
- Bảng và cột trong MySQL: `snake_case`, tên bảng số ít hoặc số nhiều thì **chọn một kiểu cho cả nhóm**: `______`.
- Template Thymeleaf: `templates/<tính-năng>/<hành-động>.html` (ví dụ `product/list.html`, `product/form.html`).
- File TypeScript: `ts/<tính-năng>/<tên>.ts`.

---

## 8. TypeScript

Mã nguồn viết trong `src/main/ts/`, biên dịch ra `src/main/resources/static/js/` (thư mục này nằm trong `.gitignore`).

```bash
npm install          # lần đầu sau khi clone
npm run build        # biên dịch một lần
npm run watch        # biên dịch tự động khi đang code
```

`package.json` cần có:

```json
"scripts": {
  "build": "tsc",
  "watch": "tsc --watch"
}
```

Nếu thấy trang thiếu JavaScript sau khi clone, bạn chưa chạy `npm run build`.

---

## 9. Cấu hình và cơ sở dữ liệu

**Cấu hình cá nhân (mật khẩu DB):**

- `application-example.properties` được commit, chứa giá trị giả để làm mẫu.
- Mỗi người tự tạo `application-local.properties` với thông tin MySQL của mình. File này **không bao giờ commit**.

**Cấu trúc bảng:**

- `database/schema.sql` là **nguồn chuẩn** của cấu trúc bảng. Đặt `spring.jpa.hibernate.ddl-auto=validate` để Hibernate chỉ kiểm tra, không tự sửa bảng.
- Muốn đổi cấu trúc bảng (thêm bảng, thêm cột, đổi kiểu): tạo **PR riêng, nhỏ**, báo cả nhóm trong nhóm chat, và gộp sớm để người khác cập nhật.
- Sau khi kéo code mới có đổi `schema.sql`, chạy lại script trên MySQL của mình.

---

## 10. Kiểm thử

- **JUnit**: test đặt trong `src/test/java`, cùng package với lớp được test. `ProductService` thì test là `ProductServiceTest`. Ưu tiên test cho lớp Service.
- **Postman**: collection lưu trong `docs/postman/`, chia folder theo tính năng. Không lưu mật khẩu hay token thật trong file environment.

---

## 11. Phân công và file dùng chung

**Người phụ trách chính của từng tính năng** (điền vào):

| Tính năng | Người phụ trách |
|---|---|
| Đăng nhập, phân quyền | |
| Sản phẩm | |
| Khuyến mãi | |
| Bán hàng tại quầy | |
| Hóa đơn | |
| Ca làm việc | |

- Cần sửa phần của người khác: nhắn cho người đó, hoặc làm PR nhỏ và gắn họ review.
- **File dùng chung dễ đụng nhau**: `config/`, `templates/layout/`, `pom.xml`, `database/schema.sql`, các entity gốc. Sửa các file này bằng **PR nhỏ** và báo cả nhóm trước.

---

## 12. `.gitignore` tối thiểu

```
target/
node_modules/
.idea/
*.iml
.vscode/
src/main/resources/static/js/
src/main/resources/application-local.properties
```

---

## 13. Khi gặp vấn đề

- Conflict khi `git pull`: không xóa bừa. Mở file có conflict, giữ phần đúng của cả hai bên, rồi hỏi người đã sửa phần kia nếu chưa chắc.
- Lỡ commit mật khẩu hoặc file không nên có: báo ngay cho cả nhóm, đổi mật khẩu đó, không âm thầm xóa rồi commit lại.
- Không chắc nên đặt file ở đâu hoặc đặt tên thế nào: hỏi trong nhóm chat trước khi tạo, đỡ phải sửa sau.
