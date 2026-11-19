# Test theo user story Sprint 1

Mỗi story có một file riêng. Các hàm test giữ tên tiếng Việt không dấu.
Giữ nguyên 90 ca đã có và thêm 5 ca cơ bản cho hai story hóa đơn; tổng cộng 95 ca.
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

`controller/SecurityTestSupport.java` chỉ chứa cấu hình MockMvc và dữ liệu tài khoản dùng chung,
không có hàm `@Test`. Ba file SCRUM-24/25/26 sử dụng cấu hình này. SCRUM-24 giữ cả test xác thực
trực tiếp và test request/HTML. Các file trong `service` là unit test với repository giả.
Không kết nối MySQL hay thay đổi dữ liệu ứng dụng.

Chạy toàn bộ:

```powershell
mvn.cmd -B test
```

Chạy riêng một story:

```powershell
mvn.cmd -B "-Dtest=Scrum24DangNhapTest" test
mvn.cmd -B "-Dtest=Scrum48MoCaTest" test
```

Kết quả trong `target/surefire-reports`, tách theo tên file/story.
Các test hóa đơn kiểm tra lời gọi service/repository; không chứng minh transaction hoặc snapshot trên MySQL.
