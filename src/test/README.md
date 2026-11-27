# Test theo user story Sprint 1

Mỗi story có một file riêng. Các hàm test giữ tên tiếng Việt không dấu.
Giữ nguyên 428 ca Java hiện có và thêm 60 ca SCRUM-29; tổng cộng 488 ca Java.
SCRUM-46 có thêm 6 test JavaScript, tổng hai bộ test là 494 ca.
Số ca không đồng nghĩa đã bao phủ đầy đủ mọi acceptance criteria.

File trong bảng tính từ `src/test/java/pos/`.

| Jira | User story | File test | Số ca |
|---|---|---|---:|
| SCRUM-45 | Tìm/lọc hóa đơn, phạm vi tài khoản và phân trang | `service/Scrum45TraCuuHoaDonTest.java` | 36 |
| SCRUM-45 | Render bộ lọc/kết quả, quyền HTTP và liên kết chi tiết | `controller/Scrum45TraCuuHoaDonViewTest.java` | 17 |
| SCRUM-45 | Điều kiện JPQL, bind/escape mã và snapshot DTO | `repository/Scrum45TruyVanHoaDonTest.java` | 7 |
| SCRUM-46 | Snapshot, phạm vi tài khoản, in lặp chỉ đọc | `service/Scrum46InHoaDonTest.java` | 18 |
| SCRUM-46 | Xem trước 80 mm, link in lại, thanh toán và quyền | `controller/Scrum46InHoaDonViewTest.java` | 17 |
| SCRUM-24 | US-DN-01 Đăng nhập | `controller/Scrum24DangNhapTest.java` | 10 |
| SCRUM-25 | US-DN-02 Đăng xuất | `controller/Scrum25DangXuatTest.java` | 3 |
| SCRUM-26 | US-DN-03 Phân quyền | `controller/Scrum26PhanQuyenTest.java` | 11 |
| SCRUM-27 | US-DN-04 Tạo và khóa tài khoản | `service/Scrum27TaoVaKhoaTaiKhoanTest.java` | 14 |
| SCRUM-28 | US-SP-01 Thêm sản phẩm | `service/Scrum28ThemSanPhamTest.java` | 17 |
| SCRUM-29 | Sửa sản phẩm, validation, mã trùng, quyền, snapshot và phiên bản form | `service/Scrum29SuaSanPhamTest.java` | 42 |
| SCRUM-29 | Form sửa, lỗi dữ liệu, CSRF và quyền HTTP | `controller/Scrum29SuaSanPhamViewTest.java` | 15 |
| SCRUM-29 | Ghi thuộc tính, giữ trạng thái và biến động kho | `repository/Scrum29LuuSanPhamTest.java` | 3 |
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

SCRUM-45 kiểm tra 8 tổ hợp có/không có mã, phương thức, trạng thái; thu ngân chỉ truyền phạm vi chính mình,
Quản lý/Admin xem toàn bộ; từ chối tài khoản khóa/không tồn tại/khác vai trò; ID/vai trò do client gửi
không mở rộng phạm vi. Kiểm tra đầu vào sai, mã dài, trang âm/tràn, 0/1/20/21 kết quả;
giữ số tiền/thứ tự trả về, phân trang giữ bộ lọc và đúng link chi tiết.
Test repository dùng EntityManager mock, kiểm tra tham số và các điều kiện AND, escape `%`/`_`/`!`,
không nối mã người dùng vào JPQL, không thêm H2 hoặc ghi dữ liệu.
Smoke test trên MySQL local: MANAGER/ADMIN thấy 9 hóa đơn (8 CASH, 1 BANK_TRANSFER), CASHIER thấy 7 hóa đơn
(6 CASH, 1 BANK_TRANSFER), hóa đơn khác người lập bị 403; mã đầy đủ viết thường khớp đúng 1 hóa đơn;
mã không tồn tại/ký tự wildcard trả danh sách rỗng 200. VOID chưa có dữ liệu thực tế trong DB local,
hiển thị chi tiết VOID được kiểm tra bằng MockMvc. Smoke chỉ tra cứu, không sửa hóa đơn.

SCRUM-46 lấy giá/tên/đơn vị/giảm giá từ snapshot; kiểm tra tiền thập phân và số lượng lẻ,
MANAGER/ADMIN/thu ngân đúng người lập, tài khoản khóa/khác vai trò/không tồn tại, mã ca không cần mở lại,
ID sai/hóa đơn chưa lưu; gọi xem trước lặp giữ nguyên dữ liệu và không gọi thao tác ghi.
MockMvc kiểm tra giấy cuộn 80 mm, nội dung in, đánh dấu VOID, escape tên hàng, từ chối khổ giấy khác,
link in từ chi tiết, redirect sau thanh toán và từ chối POST vào endpoint xem trước.
Test JavaScript ở `src/test/js/invoice-print.test.cjs` chạy mã đã biên dịch:
chờ font, chặn bấm lặp, đóng/hủy hộp thoại cho phép in lại, lỗi print API/font và thiếu nút.
Không gọi API ghi dữ liệu từ script. Mô phỏng đóng hộp thoại không chứng minh máy in vật lý đã in thành công.
Smoke MySQL local: MANAGER/ADMIN/CASHIER mở được bản giấy cuộn 80 mm với hóa đơn trong phạm vi;
thu ngân mở bản xem trước hóa đơn khác người lập nhận 403. Chỉ đọc dữ liệu hóa đơn có sẵn.
Bản in PDF thử bằng Chrome headless gồm một dòng thực tế và fixture 60 dòng/tên dài;
khổ giấy cuộn mô phỏng driver 80 × 297 mm. Cần nghiệm thu driver/máy in thật.

SCRUM-29 dùng chung validation với thêm sản phẩm, giữ 17 test SCRUM-28 chạy đạt.
Bao phủ dữ liệu hợp lệ, thiếu/sai/vượt giới hạn, giá không dương, số lượng theo độ chính xác đơn vị,
tồn thấp hơn lượng giữ, mã vạch trùng barcode/SKU khác (kể cả sản phẩm ngừng bán), loại trừ chính bản ghi;
URL ảnh, đổi đơn vị có/chưa có lịch sử, form cũ, quyền và CSRF, lưu chênh lệch kho tăng/giảm/không đổi.
Test mock không chứng minh transaction và constraint MySQL thực tế.
Đã kiểm tra bổ sung bằng MySQL local trong transaction rollback: cập nhật tên/giá/tồn sản phẩm,
tạo đơn/dòng bán mới qua service lấy giá mới; hóa đơn và DTO in cũ giữ nguyên;
sau rollback so sánh sản phẩm và số lượng đơn/ca/hóa đơn/movement trở về dữ liệu ban đầu.
HTTP thật: MANAGER mở form 200, gửi giá 0 trả 400 và giữ sản phẩm; CASHIER/ADMIN mở form trả 403.
Helper kiểm tra DB là công cụ tạm trong target, không nằm trong bộ unit test mặc định.

Chạy toàn bộ:

```powershell
mvn.cmd -B test
npm.cmd run test:print
```

Chạy riêng một story:

```powershell
mvn.cmd -B "-Dtest=Scrum24DangNhapTest" test
mvn.cmd -B "-Dtest=Scrum29*" test
mvn.cmd -B "-Dtest=Scrum45*" test
mvn.cmd -B "-Dtest=Scrum46*" test
mvn.cmd -B "-Dtest=Scrum48MoCaTest" test
mvn.cmd -B "-Dtest=Scrum49TomTatCaTest" test
mvn.cmd -B "-Dtest=Scrum50KetThucCaTest,Scrum50KetThucCaViewTest,Scrum50LuuKetQuaCaTest" test
mvn.cmd -B "-Dtest=Scrum51*" test
mvn.cmd -B "-Dtest=Scrum52*" test
```

Kết quả trong `target/surefire-reports`, tách theo tên file/story.
Các test hóa đơn kiểm tra lời gọi service/repository; không chứng minh transaction hoặc snapshot trên MySQL.
