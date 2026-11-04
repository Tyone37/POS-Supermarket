# Kiểm tra bản demo

Thực hiện trên bản HTML độc lập và source ngày 04/11/2026. Đây là kết quả prototype, không phải kiểm thử tích hợp Spring Boot/MySQL.

## Kiểm tra tự động

`node --check app.js` không có lỗi cú pháp.

`node verify-demo.cjs`: **17 kiểm tra đạt**, gồm:

- Tổng và từng dòng của 27 hóa đơn mẫu, tiền khách đưa đủ.
- Tổng doanh thu tuần 6.133.800 ₫; tổng theo sản phẩm và theo ngày bằng tổng hóa đơn nguồn.
- Kỳ không dữ liệu bằng 0, bao gồm cả hai ngày biên.
- Khuyến mãi đang chạy/sắp chạy/kết thúc, chỉ khuyến mãi hiệu lực áp dụng.
- Ví dụ 84.000 − 6.400 = 77.600 ₫.
- Lập giỏ chưa trừ tồn, hóa đơn cũ không đổi khi sửa giá/chương trình.
- Phạm vi thu ngân đăng nhập, route quản trị không thuộc thu ngân.
- Các hóa đơn mẫu đều có ca tương ứng, đúng thu ngân và quầy.
- Tìm tên không dấu, bao gồm ký tự Đ hoa.
- Tiền dự kiến dùng doanh thu phải trả, không dùng tổng tiền khách đưa.

## Thử trên trình duyệt

- Mở thành công bản gộp `pos-supermarket-demo.html`, không có stylesheet/script bên ngoài.
- Luồng thêm 2 Sữa + 1 Bánh, thu 77.600 ₫, nhập 50.000 bị chặn; nhập 100.000 cho tiền thừa 22.400; mở hóa đơn thành công, giỏ rỗng và tồn sữa giảm 2.
- Doanh thu ca tăng đúng hóa đơn vừa hoàn tất; đối soát 734.600 ₫ theo kịch bản đã thử, kết thúc và vào bán hàng thì yêu cầu mở ca mới.
- Tổng hóa đơn nguồn 27 hóa đơn là 6.133.800 ₫, khớp báo cáo ban đầu.
- Kỳ 20–21/11/2026 hiển thị 0 doanh thu, 0 hóa đơn, 0 trung bình.
- Tên tài khoản trùng được báo lỗi; chương trình khuyến mãi của Sữa giao khoảng 04–07/11 với chương trình hiện tại bị chặn.
- Đăng nhập `thungan.hoa` bị từ chối vì khóa; sai mật khẩu bị báo lỗi; `thungan.binh` đăng nhập hiển thị Lê Thanh Bình / Quầy 02.
- Manrope được áp dụng. Kiểu chữ đã được cập nhật sang nhiều độ đậm theo yêu cầu mới; xem phần cập nhật cuối tài liệu.
- Đã mở và đo ở chiều rộng CSS **375px**: tổng quan, sản phẩm, khuyến mãi, ca, hóa đơn, đơn hàng, trả/đổi, tài khoản, báo cáo, POS và hộp thanh toán. Trang không tràn ngang; bảng quản lý hiển thị thành dòng có nhãn. Hàng chip ngành hàng có vùng cuộn ngang riêng.
- Đã xem dashboard và POS ở desktop; kiểm tra dashboard chiều rộng CSS 1280px. Lỗi intrinsic width của lưới POS mobile đã sửa bằng minmax(0, 1fr) và đo lại: document scrollWidth 367, innerWidth 375; cart rộng khoảng 343px, không tràn.
- Không ghi nhận console error khi mở và chuyển màn trong lượt thử đã kiểm tra.

## Giới hạn xác minh

Chưa thử máy quét USB, máy in vật lý hoặc đầu ra PDF của mọi máy in. Nút In dùng hộp thoại in trình duyệt và CSS in. Chưa kiểm tra API, MySQL, giao dịch đồng thời, khóa nhiều máy, thu hồi phiên thật hoặc cơ chế chống ghi lặp sau tải lại. Các phần này cần kiểm thử khi ghép vào ứng dụng máy chủ. Bản demo không lưu thay đổi qua lần tải lại.

## Kiểm tra sau cập nhật nhận diện

- Tên trang, sidebar, nội dung và hóa đơn dùng POS - Supermaket; không còn tên thương hiệu cũ trong mã HTML/JS/CSS hiện hành.
- Đã xác nhận trên trình duyệt: font Manrope; nội dung 400, nút 600, tiêu đề trang 700; nút chính rgb(37, 99, 235).
- document.fonts.check xác nhận 400/500/600/700 đều sẵn sàng.
- Dashboard và POS ở chiều rộng CSS 375px: scrollWidth 367px, không tràn trang; cart khoảng 343px.
- 17 kiểm tra dữ liệu/luồng tính vẫn đạt; JavaScript không có lỗi cú pháp.

## Kiểm tra sau hoàn thiện UI/motion

- 18 kiểm tra dữ liệu và chính sách giảm chuyển động đạt. Giá trị tiền được giữ nguyên trong lúc các phần tử xuất hiện.
- Đã thử tìm nhanh tên không dấu bằng bàn phím: tìm `san pham`, ArrowDown, Enter mở đúng màn và focus vào nội dung. Tìm trên trang sản phẩm không chạy lại hiệu ứng vào trang.
- Menu tìm nhanh của Thu ngân không có Sản phẩm/Khuyến mãi/Báo cáo/Tài khoản.
- Đã kiểm tra lại 11 màn ở CSS 375px: dashboard, sản phẩm, khuyến mãi, ca, hóa đơn, đơn hàng, trả/đổi, báo cáo, POS, tài khoản và phân quyền. Document scrollWidth 367px, không tràn ngang.
- Đã thử giỏ 2 Sữa + 1 Bánh sau thay đổi: tổng 77.600 ₫; cập nhật giỏ không lặp animation trang. Hộp thanh toán ở mobile rộng khoảng 333px, không tràn nội dung; con trỏ tự đặt vào ô Tiền khách đưa.
- Đã thử menu mobile: nền được phủ, cuộn trang sau bị khóa; Shift+Tab từ logo quay tới Đăng xuất; Escape đóng menu, bỏ khóa cuộn và trả focus về Mở menu.
- Không ghi nhận console error trong lượt kiểm tra các tương tác mới. Chưa thay đổi hoặc kiểm thử backend/MySQL.

## Điều chỉnh tập trung quầy POS

- Không còn khối storecard trên giao diện; chữ POS góc trái có chiều cao một dòng (30px trên rail desktop, 36px khi mở menu mobile).
- Nút Đăng xuất có nền rgb(217,54,73); hộp xác nhận có Ở lại và Đăng xuất, mô tả giỏ đang lập và ca còn mở. Đã thử Ở lại: giữ nguyên giỏ 77.600 ₫ và phiên; xác nhận Đăng xuất: về đăng nhập.
- Mặc định mở màn Bán hàng của Thu ngân. Rail nhỏ; scanner tự nhận focus; danh mục cuộn riêng, thẻ đủ chiều cao và giỏ cố định trong vùng bán hàng desktop.
- Đã quét/nhập SP001 hai lần và SP002 một lần: tổng 77.600 ₫; sau mỗi lần quét, focus trở lại barcode. F2 từ ô tìm kiếm về barcode; F4 mở thanh toán đúng số tiền.
- Desktop kiểm tra: thanh toán nằm trong viewport (đáy nút khoảng 798px ở viewport cao 818px); các thẻ sản phẩm cao tối thiểu 220px, danh mục có thể cuộn.
- Mobile CSS 375px: document scrollWidth 367px, không tràn trang; menu và xác nhận đăng xuất vẫn sử dụng được.

## Dropdown tùy biến

- Đã thay UI select mặc định ở header, bộ lọc và modal; native select ẩn nhưng giữ giá trị form/handler.
- Vai trò: ArrowDown, ArrowUp, Enter chọn Quản lý từ Thu ngân; mở đúng Tổng quan và đồng bộ giá trị manager.
- Sản phẩm: chọn Ngừng bán ở bộ lọc mới trả về 1 dòng và native value false; không còn select mặc định hiển thị.
- Modal khuyến mãi: dropdown 9 sản phẩm có tìm kiếm; tìm aquafina và chọn SP003, giá trị cập nhật đúng, modal vẫn mở.
- Escape trong dropdown trả focus về trigger và giữ form mở; menu có animation đóng 130ms.
- Đã đo menu sản phẩm bên trong modal và menu vai trò ở CSS 375px: nằm trong viewport, không bị modal cắt; document scrollWidth 367px.
- Không ghi nhận console error trong lượt kiểm tra. Kiểm tra cú pháp và 18 kiểm tra logic/motion vẫn đạt.
