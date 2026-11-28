# Test theo user story Sprint 1

Mỗi story có một file riêng. Các hàm test giữ tên tiếng Việt không dấu.
Tổng cộng 665 ca Java, gồm 9 ca sửa lỗi đọc khuyến mãi, 10 ca sửa form giảm giá trực tiếp và 80 ca bổ sung cho coupon cùng bảo vệ sản phẩm liên quan.
SCRUM-46 có thêm 6 test JavaScript, tổng hai bộ test là 671 ca.
Số ca không đồng nghĩa đã bao phủ đầy đủ mọi acceptance criteria.

File trong bảng tính từ `src/test/java/pos/`.

| Jira | User story | File test | Số ca |
|---|---|---|---:|
| Coupon | Phân bổ toàn đơn/sản phẩm, số lượng, điều kiện và làm tròn | `service/CouponDiscountCalculatorTest.java` | 20 |
| Coupon | Quản lý mã, phạm vi, sản phẩm, thời gian, phân trang và quyền | `service/CouponServiceTest.java` | 34 |
| Coupon | Thay thế ưu đãi, sửa giỏ và chặn thanh toán coupon cũ | `service/CouponSaleServiceTest.java` | 12 |
| Coupon | Form/list/edit, ràng buộc, CSRF và quyền HTTP | `controller/CouponControllerTest.java` | 13 |
| Sửa lỗi khuyến mãi | Danh sách/chi tiết chỉ đọc không khóa ghi, vẫn kiểm tra quyền | `service/PromotionReadOnlyTest.java` | 9 |
| tin-lam | Tạo khuyến mãi | `service/UsKm01TaoKhuyenMaiTest.java` | 5 |
| tin-lam | Chỉnh sửa/hủy khuyến mãi | `service/UsKm02ChinhSuaHuyKhuyenMaiTest.java` | 8 |
| tin-lam | Tự động áp dụng khuyến mãi | `service/UsKm03ApDungKhuyenMaiTest.java` | 5 |
| tin-lam | Giảm giá đơn hàng, checkbox vắng mặt và mức giảm trống | `service/U14GiamGiaDonHangTest.java` | 9 |
| Sửa lỗi form giảm giá | Binding trường tùy chọn, checkbox và lỗi nhập liệu | `controller/DirectDiscountFormViewTest.java` | 7 |
| tin-lam + coupon | Xóa sản phẩm, chặn sản phẩm được coupon tham chiếu | `service/UsSp04XoaSanPhamTest.java` | 6 |
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

Sửa lỗi khuyến mãi: `PromotionService.list/get` kiểm tra tài khoản không dùng khóa ghi trong transaction chỉ đọc.
Các tác vụ ghi vẫn dùng cách khóa cũ. Test bao phủ quyền Manager, tài khoản khóa/không tồn tại,
vai trò khác, danh sách rỗng/có dữ liệu và chi tiết không tồn tại/có sản phẩm áp dụng.
Đã kiểm tra lại với MySQL local và đăng nhập HTTP bằng Manager: `/promotions` trả 200;
database kiểm tra hiện không có chương trình khuyến mãi, lượt kiểm tra không thêm/sửa dữ liệu.

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
