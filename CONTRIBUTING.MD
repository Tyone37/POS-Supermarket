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

## 5. Khi gặp vấn đề

- Conflict khi `git pull`: không xóa bừa. Mở file có conflict, giữ phần đúng của cả hai bên, rồi hỏi người đã sửa phần kia nếu chưa chắc.
- Lỡ commit mật khẩu hoặc file không nên có: báo ngay cho cả nhóm, đổi mật khẩu đó, không âm thầm xóa rồi commit lại.
- Không chắc nên đặt file ở đâu hoặc đặt tên thế nào: hỏi trong nhóm chat trước khi tạo, đỡ phải sửa sau.
