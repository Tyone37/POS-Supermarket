# Test theo user story Sprint 1

Mỗi story có một file riêng. Các hàm test giữ tên tiếng Việt không dấu.
Giữ nguyên 266 ca hiện có và thêm 67 ca SCRUM-52; tổng cộng 333 ca.
Số ca không đồng nghĩa đã bao phủ đầy đủ mọi acceptance criteria.

File trong bảng tính từ `src/test/java/pos/`.

| Jira | User story | File test | Số ca |
|---|---|---|---:|
| SCRUM-24 | US-DN-01 Đăng nhập | `controller/Scrum24DangNhapTest.java` | 10 |
| SCRUM-25 | US-DN-02 Đăng xuất | `controller/Scrum25DangXuatTest.java` | 3 |
| SCRUM-26 | US-DN-03 Phân quyền | `controller/Scrum26PhanQuyenTest.java` | 11 |
| SCRUM-27 | US-DN-04 Tạo và khóa tài khoản | `service/Scrum27TaoVaKhoaTaiKhoanTest.java` | 14 |
| SCRUM-28 | US-SP-01 Thêm sản phẩm | `service/Scrum28ThemSanPhamTest.java` | 17 |
| SCRUM-35 | U1.1 Quét/thêm sản phẩm vào đơn | `service/Scrum35ThemSanPhamVaoDonTest.java` | 1 |
| SCRUM-36 | U1.2 Tìm kiếm sản phẩm | `service/Scrum36TimSanPhamTest.java` | 4 |
| SCRUM-37 | U1.3 Điều chỉnh số lượng/xóa dòng | `service/Scrum37DieuChinhSoLuongTest.java` | 7 |
| SCRUM-40 | U1.6 Hủy đơn | `service/Scrum40HuyDonTest.java` | 2 |
| SCRUM-42 | U3.1 Thanh toán | `service/Scrum42ThanhToanTest.java` | 5 |
| SCRUM-43 | U4.1 Tạo hóa đơn | `service/Scrum43TaoHoaDonTest.java` | 2 |
| SCRUM-44 | U4.2 Xem hóa đơn | `service/Scrum44XemHoaDonTest.java` | 3 |
| SCRUM-47 | US-CA-01 Xem trạng thái ca | `service/Scrum47XemTrangThaiCaTest.java` | 3 |
| SCRUM-48 | US-CA-02 Mở ca | `service/Scrum48MoCaTest.java` | 13 |
| SCRUM-49 | US-CA-03 Tóm tắt ca | `service/Scrum49TomTatCaTest.java` | 40 |
| SCRUM-49 | Render trang và quyền tóm tắt ca | `controller/Scrum49TomTatCaViewTest.java` | 4 |
| SCRUM-50 | Kết thúc/đối soát ca | `service/Scrum50KetThucCaTest.java` | 51 |
| SCRUM-50 | Form, xác nhận, CSRF và quyền | `controller/Scrum50KetThucCaViewTest.java` | 27 |
| SCRUM-50 | Lưu snapshot entity, không dùng database | `repository/Scrum50LuuKetQuaCaTest.java` | 4 |
| SCRUM-51 | Chặn giao dịch trên ca đã đóng | `service/Scrum51KhoaCaTest.java` | 27 |
| SCRUM-51 | Giữ nguyên trạng thái và số liệu chốt | `entity/Scrum51BatBienCaTest.java` | 7 |
| SCRUM-51 | Điều hướng, thanh toán muộn và xem lịch sử | `controller/Scrum51KhoaCaViewTest.java` | 11 |
| SCRUM-52 | Bộ lọc, phân trang, quyền và dữ liệu đã chốt | `service/Scrum52LichSuCaTest.java` | 48 |
| SCRUM-52 | Danh sách/chi tiết, lỗi bộ lọc, trạng thái rỗng và quyền HTTP | `controller/Scrum52LichSuCaViewTest.java` | 19 |

`controller/SecurityTestSupport.java` chỉ chứa cấu hình MockMvc và dữ liệu tài khoản dùng chung,
không có hàm `@Test`. Ba file SCRUM-24/25/26 sử dụng cấu hình này. SCRUM-24 giữ cả test xác thực
trực tiếp và test request/HTML. Các file trong `service` là unit test với repository giả.
Không kết nối MySQL hay thay đổi dữ liệu ứng dụng.
`Scrum49TomTatCaTest` có 30 hàm `@Test` và 3 hàm `@ParameterizedTest` với tổng 10 bộ dữ liệu,
tương ứng 40 ca chạy. Bao phủ ca rỗng, tiền mặt/chuyển khoản/kết hợp, tiền đầu ca,
độ chính xác BigDecimal, tổng tiền lớn, 1.000 hóa đơn, ca qua nửa đêm, tải lại trang,
chuyển ca, hai thu ngân, khóa/đổi vai trò và lỗi đọc dữ liệu tại từng bước.
Test repository H2 đã được bỏ theo yêu cầu; dependency H2 cũng được xóa.
Unit test chỉ kiểm tra tham số gọi repository và dữ liệu trả về giả lập,
không thay thế kiểm thử JPQL, CHECK/FK/unique hoặc transaction trên MySQL riêng.

SCRUM-50 bao phủ tiền âm/trống/chữ/lẻ/vượt giới hạn; ca chưa mở/đã đóng/khác chủ;
tiền mặt/chuyển khoản, hoàn tiền mặt và chênh lệch; số liệu thay đổi; token khác phiên/sai/đã dùng;
thiếu xác nhận/CSRF và sửa số tiền từ client. Test lưu entity dùng EntityManager mock, không thêm H2.
AC5 cần nghiệm thu thêm bằng các khoản hoàn thật trên DB thử riêng theo thiết kế.

SCRUM-51 kiểm tra CLOSED với DRAFT/PENDING/PAID/CANCELLED và CASH/BANK_TRANSFER;
replay payment/order/failed attempt, ca đổi trạng thái sau lượt đọc đầu, ca mới không nhận đơn cũ;
không gọi trừ tồn, inventory movement, payment, PAID hoặc invoice khi bị từ chối.
Kiểm tra chuỗi đóng ca → thanh toán muộn → gửi lại đóng ca giữ nguyên snapshot;
setter entity không sửa tiền đầu ca/giờ kết thúc/trạng thái CLOSED.
MockMvc dùng Security thật và xác minh HTTP 400, redirect mở ca và GET hóa đơn cũ.
Test mock không chứng minh khóa/rollback thực tế khi nhiều kết nối MySQL chạy đồng thời.

SCRUM-52 kiểm tra MANAGER/ADMIN, từ chối CASHIER/tài khoản khóa/không tồn tại trước khi đọc lịch sử;
lọc ngày theo giờ Việt Nam, ngày nhuận/cuối năm, khoảng một ngày, khoảng mở một đầu;
ID/trạng thái/trang không hợp lệ, phân trang 0/1/20/21 bản ghi, giữ dữ liệu chốt dù tổng hóa đơn khác;
ca OPEN/CLOSED, danh sách rỗng, hiển thị chênh lệch âm và liên kết hóa đơn.
MockMvc dùng SecurityConfig và Thymeleaf thật. Test mock không chứng minh trực tiếp điều kiện JPQL trên MySQL.
Đã chạy smoke test đọc dữ liệu MySQL local: MANAGER/ADMIN danh sách, bộ lọc kết hợp,
chi tiết 3 ca và liên kết 9 hóa đơn trả 200; CASHIER danh sách trả 403.
Không tạo/sửa/đóng ca hay hóa đơn trong lượt smoke test này.

Chạy toàn bộ:

```powershell
mvn.cmd -B test
```

Chạy riêng một story:

```powershell
mvn.cmd -B "-Dtest=Scrum24DangNhapTest" test
mvn.cmd -B "-Dtest=Scrum48MoCaTest" test
mvn.cmd -B "-Dtest=Scrum49TomTatCaTest" test
mvn.cmd -B "-Dtest=Scrum50KetThucCaTest,Scrum50KetThucCaViewTest,Scrum50LuuKetQuaCaTest" test
mvn.cmd -B "-Dtest=Scrum51*" test
mvn.cmd -B "-Dtest=Scrum52*" test
```

Kết quả trong `target/surefire-reports`, tách theo tên file/story.
Các test hóa đơn kiểm tra lời gọi service/repository; không chứng minh transaction hoặc snapshot trên MySQL.
