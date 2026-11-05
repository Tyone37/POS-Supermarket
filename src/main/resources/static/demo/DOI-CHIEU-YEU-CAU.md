# Đối chiếu tài liệu và demo

## Nguồn sử dụng

Nguồn gốc: `D:\KY_1_NAM_4\Professional Software Development`.

Đã khảo sát cây thư mục; trích nội dung 21 DOCX và 66 PDF thuộc yêu cầu, story, wireframe và thiết kế dữ liệu. Các file bài giảng, bài Git mẫu và prompt hướng dẫn không được coi là yêu cầu của ứng dụng. PDF chỉ chứa ảnh có thể không trích được nội dung chi tiết, vì vậy DOCX story và wireframe dạng chữ là nguồn nghiệp vụ chính; không tuyên bố đã đối chiếu từng pixel toàn bộ PDF.

Thứ tự tham chiếu:

1. `Details dự án/PJ_Details.docx`: chức năng, mã SCRUM-24–58 và phân công ba Sprint.
2. `Final Product/PPS Xây dựng hệ thống bán hàng.docx`: ba vai trò, phạm vi siêu thị đơn, thanh toán tiền mặt, stack và quy tắc nền.
3. `Product Week 1`: story chi tiết từng thành viên về Đăng nhập/phân quyền, Ca, Báo cáo, Sản phẩm/Khuyến mãi, Bán hàng và Thanh toán/Hóa đơn.
4. `Final Product/*Chuẩn hoá.docx`: kiểm tra chéo các tiêu chí thanh toán tiền mặt, snapshot, ca và báo cáo. Mã US sản phẩm khác bản riêng của Tú; bảng dưới ưu tiên mã SCRUM trong Project Details để tránh nhầm.
5. `Product Week 2`: tham chiếu tên màn/trạng thái và nội dung wireframe trích được.
6. `SyncDrive/Tong hop chung`: đối chiếu nguồn báo cáo, mô hình và các điểm chưa chốt. Các lựa chọn thiết kế trong thư mục này vẫn là đề xuất, không khẳng định cả nhóm đã duyệt.
7. `Project/POS-Supermarket/README.md`, CSS/template đăng nhập và cấu trúc code hiện có: xác định stack và ranh giới tích hợp. Không chỉnh repo này hoặc bản `POS-Supermarket` còn lại.

## Ánh xạ story → giao diện

| Story | Màn / trạng thái trong demo | Mức độ |
|---|---|---|
| SCRUM-24 / US-DN-01 | Đăng nhập đúng/sai, tài khoản khóa; chuyển trang theo vai trò | Mô phỏng trên trình duyệt |
| SCRUM-25 / US-DN-02 | Đăng xuất, trở về đăng nhập, không mở route riêng khi vẫn chưa đăng nhập | Mô phỏng; không có phiên máy chủ |
| SCRUM-26 / US-DN-03 | Menu và route theo ba vai trò, trang ma trận phân quyền | Mô phỏng hiển thị; cần Spring Security |
| SCRUM-27 / US-DN-04 | Danh sách tài khoản, tạo tài khoản có vai trò, chặn tên trùng, khóa tài khoản, giữ lịch sử | Dữ liệu trong bộ nhớ |
| SCRUM-28 / US-SP-01 của Tú | Danh sách, form thêm sản phẩm, bắt buộc, mã vạch trùng, giá/tồn hợp lệ | Dữ liệu trong bộ nhớ |
| SCRUM-29 / US-SP-02 của Tú | Sửa sản phẩm; hóa đơn dùng snapshot nên giá cũ không đổi | Dữ liệu trong bộ nhớ |
| SCRUM-30 / US-SP-03 của Tú | Ngừng bán/kích hoạt lại; không thêm hàng ngừng bán vào giỏ | Mô phỏng |
| SCRUM-31 / US-SP-04 của Tú | Chỉ xóa hàng ngừng bán chưa có giao dịch; chặn xóa có lịch sử, xác nhận trước xóa | Mô phỏng |
| SCRUM-32 / US-KM-01 | Tạo giảm % theo sản phẩm, khoảng ngày, chặn lịch trùng | Mô phỏng |
| SCRUM-33 / US-KM-02 | Sửa chương trình sắp diễn ra, hủy chương trình đang áp dụng, xem chương trình kết thúc | Mô phỏng |
| SCRUM-34 / US-KM-03 | Áp dụng giảm đang hiệu lực; không dùng chương trình sắp diễn ra/kết thúc; lưu giảm vào hóa đơn | Mô phỏng |
| SCRUM-35 / U1.1 | Quét/nhập mã bằng bàn phím, thêm đúng hàng, nhập lại tăng lượng, mã sai thông báo | Không kết nối scanner thực |
| SCRUM-36 / U1.2 | Tìm tên không dấu/mã/mã vạch; lọc nhóm; minh họa, giá, tồn | Mô phỏng |
| SCRUM-37 / U1.3 | Tăng/giảm, nhập số lượng, chặn vượt tồn, xác nhận xóa dòng | Mô phỏng; chưa trừ tồn khi lập giỏ |
| SCRUM-38 / U1.4 | Hiển thị giảm theo dòng và tổng giảm theo chương trình sản phẩm | Theo PPS; coupon/giảm toàn đơn chưa đưa vào |
| SCRUM-39 + 42 / U1.5 + Thanh toán | Tóm tắt, tiền khách đưa, chặn thiếu, tiền thừa, xác nhận, trạng thái thành công | Tiền mặt giả lập |
| SCRUM-40 / U1.6 | Hủy đơn chưa thanh toán, xác nhận, lý do và lịch sử | Mô phỏng |
| SCRUM-41 / U1.7 | Tra hóa đơn, chọn SL không vượt đã mua, hàng đổi mới tùy chọn, chênh lệch, gửi/duyệt quản lý | Chỉ minh họa đề nghị/phê duyệt; không hạch toán |
| SCRUM-43, 44 / Hóa đơn | Tạo và xem hóa đơn sau giao dịch demo; dòng hàng, giá, lượng, giảm, tiền nhận/thừa, ca, thu ngân | Dữ liệu trong bộ nhớ; in không sửa tồn |
| SCRUM-45 / Tra cứu hóa đơn | Mã/ngày, chỉ hóa đơn đã phát hành; phạm vi thu ngân của mình/quản lý | Mô phỏng |
| SCRUM-46 / In hóa đơn | Hóa đơn và CSS in khổ hẹp, nút mở hộp thoại in trình duyệt | Không yêu cầu máy in vật lý |
| SCRUM-47 / US-CA-01 | Chưa mở, đang mở, kết thúc; bán hàng khi có ca mở | Mô phỏng |
| SCRUM-48 / US-CA-02 | Thu ngân, quầy, giờ mở, tiền đầu ca không âm; chặn mở lặp | Bộ nhớ đơn trình duyệt |
| SCRUM-49 / US-CA-03 | Giờ mở, tiền đầu ca, số hóa đơn/doanh thu đúng ca | Cùng nguồn hóa đơn |
| SCRUM-50 / US-CA-04 | Tiền dự kiến = đầu ca + doanh thu tiền mặt; thực tế và chênh lệch; xác nhận trước kết thúc | Mô phỏng |
| SCRUM-51 / US-CA-05 | Ca kết thúc bị khóa; phải mở ca mới để bán | Không thay thế khóa/transaction phía máy chủ |
| SCRUM-52 / US-CA-06 | Lọc ngày, thu ngân, quầy, trạng thái; chi tiết đối soát và hóa đơn cùng ca | Mô phỏng |
| SCRUM-53 / US-BC-01 | Lọc ngày/khoảng ngày, gồm hai biên; báo ngược ngày; kỳ rỗng bằng 0 | Demo giới hạn 92 ngày để giữ giao diện gọn |
| SCRUM-54 / US-BC-02 | Tổng sau giảm của hóa đơn thành công, không cộng tiền đầu ca/tiền nhận/thừa | Cùng tập dữ liệu mẫu |
| SCRUM-55 / US-BC-03 | Đếm hóa đơn; trung bình = tổng / đếm; 0 nếu không có | Hiển thị VND làm tròn nguyên đồng |
| SCRUM-56 / US-BC-04 | Xếp lượng giảm dần, tên/mã, lượng và doanh thu sau giảm | Snapshot hóa đơn; không phân tích thời gian thực |
| SCRUM-57 / US-BC-05 | Đủ các ngày, có 0; trên 14 ngày chuyển bảng để đọc được | Tối đa 92 ngày mỗi lần |
| SCRUM-58 / US-BC-06 | Đối chiếu hóa đơn theo cùng bộ lọc, tổng danh sách khớp báo cáo, xem chi tiết | Mô phỏng |

## Những khác biệt tài liệu được xử lý rõ

- **Tiền mặt / QR / thẻ:** PPS và backlog chuẩn hóa chọn tiền mặt; story thanh toán/bán hàng cũ có QR/thẻ. Demo chỉ dùng tiền mặt, không vẽ QR giả hoặc tuyên bố tích hợp ngân hàng.
- **Coupon / giảm toàn đơn:** story bán hàng cũ có coupon và duyệt giảm vượt hạn mức. PPS và story Tú quy định giảm % trên sản phẩm. Demo chọn cách sau; không coi hai phạm vi đã được nhóm thống nhất.
- **Đổi trả:** kế hoạch chi tiết có SCRUM-41 ở Sprint 3; tài liệu nguồn báo cáo chưa chốt cách điều chỉnh. Vì vậy có màn đề nghị và phê duyệt minh họa, chưa thay đổi hóa đơn gốc, tồn hoặc doanh thu.
- **VAT / điểm thành viên / chi nhánh:** vài mockup có thông tin này nhưng PPS không đưa vào phạm vi cơ bản. Demo không thêm thuế riêng, điểm thành viên hoặc chọn chi nhánh.
- **Bảo mật và tính toàn vẹn:** story yêu cầu xử lý máy chủ; demo không tuyên bố đáp ứng kiểm tra quyền API, băm mật khẩu, BigDecimal, atomic transaction hay đồng thời nhiều quầy. Đây là phần nhóm cần tích hợp và kiểm thử ở Spring Boot/MySQL.

## Ngôn ngữ thiết kế

Theo yêu cầu mới nhất của người dùng: trắng chủ đạo, nút xanh biển trung tính, nền gradient nhẹ. Theo yêu cầu cập nhật: Manrope Regular/Medium/Semibold/Bold, nút xanh dương #2563eb; tên POS - Supermaket. Bố cục chung thống nhất cho các màn, responsive và trạng thái lỗi/rỗng. Các quyết định này chỉ áp dụng bản demo; không ghi đè brand hoặc thiết kế code đang phát triển.
