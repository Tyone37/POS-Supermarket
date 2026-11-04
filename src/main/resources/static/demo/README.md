# POS - Supermaket — demo giao diện cho Nhóm 06

Mở `index.html` trực tiếp bằng Chrome hoặc Edge. Không cần npm, Java hoặc MySQL. Font và hình minh họa đều nằm trong bộ demo, không phụ thuộc Internet.

Ngoài bộ source này còn có `pos-supermarket-demo.html`: bản gộp một file để gửi nhanh cho thành viên/giảng viên.

## Cách xem

Trang đầu là màn Bán hàng của Thu ngân để tập trung vào vận hành quầy POS. Chọn vai trò ở góc trên bên phải:

- **Quản lý**: Tổng quan, đơn hàng, hóa đơn, lịch sử ca, sản phẩm, khuyến mãi, báo cáo, đề nghị trả/đổi.
- **Thu ngân**: Bán hàng tại quầy, đơn hàng, hóa đơn thuộc người đăng nhập, ca của mình và đề nghị trả/đổi.
- **Quản trị viên**: Tài khoản và ma trận phân quyền.

Chọn Đăng xuất để xem màn đăng nhập. Tài khoản minh họa: `thungan.anh`, `quanly.lan`, `quantri.nam`. Mật khẩu mẫu `demo123`. `thungan.binh` dùng cùng mật khẩu để kiểm tra dữ liệu riêng của thu ngân khác. `thungan.hoa` là tài khoản bị khóa.

Ngày demo cố định **04/11/2026** để thể hiện ổn định trạng thái khuyến mãi; kỳ báo cáo mẫu **29/10–04/11/2026**. Tải lại trang sẽ trả về dữ liệu ban đầu, không lưu dữ liệu giao dịch hoặc mật khẩu vào ổ đĩa.

## Kịch bản trình bày nhanh

1. Xem Tổng quan, vào Báo cáo và bấm Đối chiếu hóa đơn: tổng nguồn phải bằng số liệu báo cáo.
2. Chọn Thu ngân. Thêm 2 Sữa tươi Vinamilk và 1 Bánh quy bơ Cosy: 84.000 − 6.400 = **77.600 ₫**.
3. Thanh toán: nhập 50.000 để xem trạng thái tiền thiếu, rồi 100.000 để thấy tiền thừa **22.400 ₫**. Xác nhận để xem hóa đơn mẫu và dùng In hóa đơn khi cần.
4. Vào Ca làm việc, nhập tiền kiểm đếm và kết thúc ca. Vào lại Bán hàng để thấy yêu cầu mở ca mới.
5. Chọn Quản lý: tạo sản phẩm, thử mã vạch trùng; tạo khuyến mãi cho Sữa từ 04/11–08/11 để thấy chặn trùng khoảng hiệu lực.
6. Chọn Quản trị viên: tạo/khóa tài khoản minh họa; thử đăng nhập tài khoản bị khóa.

## Dùng chung cho 4 thành viên

- `styles.css`: nguồn giao diện chung duy nhất, gồm token, layout, nút, ô nhập, bảng, card, modal và responsive.
- `app.js`: dữ liệu giả, hàm dựng từng màn và thao tác minh họa. Tìm các hàm `dashboard`, `pos`, `productPage`, `promotionPage`, `invoicesPage`, `shiftsPage`, `reportPage`, `accountsPage`.
- `assets/manrope-regular.ttf`: Manrope 400/500/600/700: Regular cho nội dung; Medium cho nhãn; Semibold cho nút và tiêu đề khối; Bold cho tiêu đề trang và số liệu chính.
- `DOI-CHIEU-YEU-CAU.md`: nguồn tham chiếu, mã story, phần mô phỏng và giới hạn.

Quy ước giao diện: trắng chủ đạo, xanh biển trung tính `#2563eb`, nền gradient xanh/xám rất nhạt, một họ font Manrope với các độ đậm Regular/Medium/Semibold/Bold. Tiêu đề và số liệu được nhấn bằng độ đậm, cỡ chữ và màu. Đổi màu ở `:root`; không viết lại nút/menu/bảng riêng cho từng màn.

Các lớp dùng lại: `.btn`, `.btn.primary`, `.btn.soft`, `.btn.ghost`, `.card`, `.stats`, `.field`, `.formgrid`, `.tablewrap`, `.badge`, `.notice`. Sửa CSS chung thì cả nhóm nhận cùng thay đổi. Nội dung cuối trang và bảng dài có vùng cuộn riêng.

## Ghép vào dự án hiện có

Repo được khảo sát dùng Spring Boot, Thymeleaf, Spring Security, JDBC và MySQL; tài liệu định hướng TypeScript/Bootstrap. Demo cố ý không thêm framework hay thay đổi hai bản repo trong thư mục nguồn. Khi tích hợp:

1. Tách shell/header/sidebar thành Thymeleaf fragments; đưa CSS/font vào `static`.
2. Mỗi màn là template/controller theo cấu trúc repo. Dùng token CSS trên để thống nhất cả Bootstrap nếu bổ sung.
3. Thay dữ liệu và xử lý giả bằng API/Service của nhóm. Quyền, trạng thái ca, kiểm tra tồn, tính tiền BigDecimal, idempotency và transaction phải được kiểm tra ở máy chủ.
4. Giữ dữ liệu hóa đơn bất biến tại thời điểm bán. Dùng một tập hóa đơn thành công cho doanh thu, đếm, trung bình, ngày và danh sách đối chiếu.

## Giới hạn

Đây là prototype HTML/CSS/JavaScript để duyệt giao diện và diễn giải luồng. Không phải bản nghiệm thu chức năng Spring Boot/MySQL. Chuyển vai trò là công cụ xem demo, không phải xác thực. Thanh toán không gửi tiền; không QR/thẻ/ngân hàng thật. Không bảo đảm khóa đồng thời nhiều máy, chống tải lại/đặt lại giao dịch hay transaction cơ sở dữ liệu. Trả/đổi dừng ở đề nghị/phê duyệt minh họa; chưa điều chỉnh tiền, tồn hoặc báo cáo. Không triển khai hóa đơn điện tử, nhiều chi nhánh, điểm thành viên hay VAT tách riêng.

## Giao diện đã cập nhật

Tên hiển thị thống nhất: **POS - Supermaket** theo yêu cầu người dùng. Nút xanh dương #2563eb; nút phụ viền và chữ xanh; màu đỏ chỉ giữ cho hành động xóa/lỗi. Manrope tự lưu cục bộ đủ 400/500/600/700, không tạo chữ đậm giả. Nền gradient sáng, menu chọn nền xanh, số liệu và tiêu đề có độ đậm rõ hơn.

## Hoàn thiện UI và motion

- Khung điều hướng gọn hơn; tiêu đề, số liệu, bảng, form, thẻ sản phẩm, hóa đơn và modal dùng cùng nhịp khoảng cách. Thẻ doanh thu chính có nền xanh để xác định điểm nhìn.
- Chuyển màn 350ms, khối số liệu vào lần lượt trong 480ms, biểu đồ 650ms; modal 180–220ms; phản hồi thêm hàng 260–550ms. Các số tiền vẫn hiện đúng giá trị trong suốt hiệu ứng.
- Chỉ chạy hiệu ứng vào màn khi đổi màn/vai trò. Gõ tìm kiếm, đổi bộ lọc và sửa giỏ không lặp animation toàn trang. Hover/nhấn có phản hồi nhẹ.
- Tìm nhanh: nút kính lúp ở header hoặc Ctrl+K / Cmd+K; tìm không dấu, phím ↓/↑ và Enter để đi tới màn, Esc để đóng. Kết quả chỉ có chức năng của vai trò đang chọn. Không thay form đang nhập bằng tìm nhanh.
- Mobile: menu có lớp nền, khóa cuộn phía sau, Tab giữ trong menu; Esc hoặc chạm nền để đóng, focus về nút menu. Chuyển màn đưa focus về vùng nội dung. Mở form đặt focus vào ô đầu tiên.
- Tôn trọng `prefers-reduced-motion`: tắt animation, transition và hiệu ứng thêm hàng. Không có hiệu ứng chạy lặp vô hạn hoặc phụ thuộc thư viện animation/Internet.

Các nguồn dữ liệu mẫu, story và giới hạn backend vẫn như phần trên.

## Tối ưu cho quầy POS

Góc trái dùng chữ POS một dòng, bỏ biểu tượng logo và khối thông tin cửa hàng lặp ở cuối menu. Không gian thu ngân có rail nhỏ, danh mục hàng cuộn riêng và giỏ/tiền thanh toán luôn thấy trên desktop. Nhập/quét mã được ưu tiên và tự nhận focus; F2 trở lại ô mã, F4 mở thanh toán. Đăng xuất màu đỏ, cần xác nhận; nếu đang có giỏ hoặc ca mở thì có thông tin nhắc trước khi rời phiên. Chọn Ở lại giữ nguyên phiên và giỏ.

## Dropdown và trạng thái chọn

Tất cả select được nâng cấp thành combobox tùy biến, gồm chọn vai trò, bộ lọc và các form trong modal. Menu dùng native Popover top layer để không bị card/modal cắt; tự căn mép màn hình và lật lên khi thiếu chỗ. Native select được giữ ẩn để bảo toàn giá trị và handler thay đổi.

Menu có hover, dấu chọn, icon/mô tả vai trò, mũi tên xoay, mở 190ms và đóng 130ms. Danh sách trên 6 mục có tìm không dấu. Dùng ArrowUp/Down, Home/End, Enter, Escape và Tab; click bên ngoài đóng menu. Hiệu ứng tắt khi bật giảm chuyển động. Tab nhóm hàng, vai trò đăng nhập, các nút chọn và lỗi nhập có phản hồi nhẹ, không lặp animation toàn màn khi lọc/sửa giỏ.
