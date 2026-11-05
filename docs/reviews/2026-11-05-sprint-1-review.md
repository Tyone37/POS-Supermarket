# Đối chiếu Sprint 1 — 05/11/2026

## Kết luận

14/14 story trong ảnh có đường đi chức năng trong mã nguồn hiện tại; chưa đủ bằng chứng để đánh dấu 14/14 đã nghiệm thu toàn bộ AC. Đã kiểm tra đọc trang, tìm kiếm, đăng nhập/đăng xuất và một số ranh giới quyền trên bản chạy cổng 8081; các ca ghi dữ liệu, đồng thời và rollback cần người phụ trách kiểm thử chạy trên dữ liệu thử riêng. Không thay đổi mã nghiệp vụ trong lượt rà soát này.

## Phạm vi và nguồn

- Branch: `refactor/cap-nhat-giao-dien`, bao gồm thay đổi chưa commit.
- Ảnh Jira hiện tại: 14 story, tổng điểm các dòng là 52 SP, khác nhãn sprint 60 SP. Cần cập nhật lại scope/ước lượng trên Jira nếu 52 SP là phạm vi cuối.
- `docs/superpowers/specs/2026-11-04-sprint-1-design.md`: tiêu chí tóm tắt lưu trong dự án; tài liệu này cũ, có thêm SCRUM-39.
- `docs/superpowers/plans/2026-11-04-sprint-1.md`: ghi rõ phạm vi thanh toán đã được thống nhất lại là tiền mặt/chuyển khoản xác nhận thủ công, thay cho đề xuất thẻ/ví cũ; kiểm thử giao cho người khác.
- README, controller/service/repository/config/template và schema được đối chiếu. Không có nội dung AC mới nhất của từng ticket Jira trong ảnh; các kết luận AC phải được xác nhận với ticket đầy đủ trước khi chuyển Done.
- Không dùng hành vi của `/demo` làm bằng chứng nghiệp vụ thật.

## Ma trận 14 story

“Có triển khai” mô tả code/luồng hiện có, không thay cho kết quả kiểm thử toàn bộ AC.

| Jira | Story | Triển khai và bằng chứng | Phần cần nghiệm thu thêm |
|---|---|---|---|
| SCRUM-24 | Đăng nhập hợp lệ | Có triển khai BCrypt, trạng thái tài khoản, chuyển theo vai trò. Ba tài khoản demo đăng nhập thành công; sai mật khẩu trở về `error=invalid`. | Tài khoản khóa, biên dữ liệu, hành vi trình duyệt. |
| SCRUM-25 | Đăng xuất | Có POST logout, CSRF, hủy session/xóa JSESSIONID, dialog xác nhận. Logout trong session kiểm tra đưa về login; URL `/sales` sau logout yêu cầu login. HTML bảo vệ có no-store. | Back/bfcache ở trình duyệt, cookie/session cũ, hủy dialog. |
| SCRUM-26 | Giới hạn theo vai trò | SecurityConfig bảo vệ server, service kiểm tra chủ sở hữu. GET và POST trái quyền có CSRF hợp lệ đều trả 403 với ba vai trò. | Thu ngân A truy cập đơn/hóa đơn B; khóa tài khoản khi đang đăng nhập. |
| SCRUM-27 | Tạo/khóa tài khoản | Có form, kiểm tra username trùng, vai trò, mật khẩu BCrypt; tạo employee cho cashier; khóa có xác nhận và chặn tự khóa. Trang admin hiển thị đúng. | Tạo thật, trùng username, khóa thật, đăng nhập lại và giữ lịch sử. |
| SCRUM-35 | Quét/thêm sản phẩm | Có barcode/SKU, gộp dòng tăng 1, kiểm tra trạng thái/tồn, tính tổng server. Catalog và giỏ hiện đúng. | Máy quét, SKU/barcode đúng/sai, mã trùng, thêm vượt tồn. |
| SCRUM-36 | Tìm sản phẩm | Có tìm tên không dấu, SKU chính xác, lọc nhóm, phân trang và tìm khi gõ. GET `cosy`, `hao hao`, `SP002` trả đúng. | Kiểm thử >20 sản phẩm, danh mục, gõ nhanh. Tìm SKU một phần chưa hỗ trợ: `SP0` không ra kết quả. |
| SCRUM-37 | Điều chỉnh/xóa dòng | Có tăng/giảm/lưu, xác nhận xóa khi về 0, tính tổng server; số lượng không còn đuôi .000 dư. | Tăng/giảm/xóa thật, hủy xác nhận, biên lượng/tồn. Dòng có giảm giá bị chặn đổi lượng theo phạm vi hiện tại. |
| SCRUM-40 | Hủy đơn đang bán | Có GET xác nhận và POST hủy, chỉ DRAFT/ca mở, giữ dòng, lý do và thời gian hủy. | Hủy thật, hủy bỏ xác nhận, từ chối đơn PAID và đơn của người khác. |
| SCRUM-42 | Thanh toán | Có CASH/BANK_TRANSFER, tiền thừa, tiền thiếu, mã GD không trùng, xác minh thủ công, chống gửi lặp và lưu thất bại. Checkout đọc được, QR đúng ảnh cung cấp. | Thanh toán thành công/thất bại thật trên DB thử; retry, gửi đồng thời, rollback, tồn sau bán. QR tĩnh không tự gắn tiền/nội dung và không có webhook ngân hàng. |
| SCRUM-43 | Tạo hóa đơn | `pay` trong transaction ghi payment, tồn, movement, PAID và invoice; invoice giữ tổng và người bán snapshot, order items giữ snapshot hàng/giá. Schema có unique order/payment. | Thanh toán tạo đúng một invoice, replay cùng key, lỗi giữa transaction không để lại dữ liệu dở dang; đổi danh mục không đổi invoice. |
| SCRUM-44 | Xem hóa đơn | Có route kiểm tra quyền, snapshot hàng/giá/lượng/giảm/tổng, tiền nhận/thừa, phương thức, trạng thái và in. GET invoice 1–3 trả 200. | So khớp tiền/từng dòng với DB; quyền chủ sở hữu và dữ liệu thay đổi sau bán. |
| SCRUM-47 | Trạng thái ca | Có chưa mở/đang mở/đã kết thúc theo ca mới nhất của thu ngân, kiểm tra ca mở trong nghiệp vụ. GET `/shifts` trả 200. | Ca chưa mở/đã đóng trên DB thử, không xem ca người khác. |
| SCRUM-48 | Mở ca/tiền đầu ca | Có chọn quầy, tiền nguyên không âm, giờ server, request key và unique ca mở theo nhân viên. | Mở ca thật, thiếu/sai quầy, tiền âm/lẻ, gửi lặp và hai request đồng thời. |
| SCRUM-28 | Thêm sản phẩm | Có barcode, tên, nhóm/đơn vị, giá/tồn, ảnh; validate và default ACTIVE, SKU tự sinh. GET manager/products trả 200. | Tạo thật, barcode trùng, sai giá/tồn, quét hàng vừa tạo. Nếu AC yêu cầu quản lý tự nhập SKU thì hiện chưa có ô đó. |

## Các điểm cần chốt trước Done

1. SCRUM-36: hiện chỉ SKU chính xác; nếu AC yêu cầu mã gần đúng thì cần sửa tìm SKU một phần. Đã tái hiện `SP002` có kết quả, `SP0` không có.
2. SCRUM-42: nhận chuyển khoản do thu ngân kiểm tra và xác nhận; không phải nhận trạng thái tự động từ ngân hàng. QR được cung cấp là mã tĩnh. Chỉ đạt khi AC chấp nhận cách này.
3. SCRUM-28: mã SKU tự sinh; cần xem lại nếu AC yêu cầu nhập SKU thủ công.
4. Nghiệm thu các ca ghi dữ liệu và tính toàn vẹn trên DB riêng; chưa được thực hiện trong lượt review, không suy ra thành công chỉ từ transaction/unique constraint hoặc build xanh.
5. Jira vẫn hiển thị In Development trong ảnh; thực hiện cập nhật trạng thái sau khi người phụ trách kiểm thử xác nhận AC.

## Chức năng ngoài 14 story

Kết thúc/đối soát ca, sửa/ngừng/xóa sản phẩm, khuyến mãi, trả hàng, báo cáo và tra cứu hóa đơn nâng cao chưa thuộc danh sách ảnh hiện tại. Các nút “sprint tiếp theo” không phải bằng chứng đã triển khai, nhưng cũng không tự động làm Sprint 1 thiếu scope. SCRUM-39/thẻ/ví không nằm trong ảnh này.

## Kiểm tra đã thực hiện trong lượt review

- Đọc code và schema, không sửa code ứng dụng.
- HTTP vào trang bảo vệ khi chưa đăng nhập đưa về login.
- Đăng nhập cashier/manager/admin; sai mật khẩu báo invalid.
- GET/POST trái quyền với CSRF hợp lệ: cashier→accounts, manager→sales, admin→products trả 403.
- Kiểm tra HTML no-store, POST logout và truy cập sau logout.
- GET các trang sales/workspace, shifts, checkout và invoice 1–3 trả 200; tìm tên không dấu và SKU chính xác.
- Không gửi yêu cầu tạo tài khoản/sản phẩm/đơn, khóa tài khoản, mở ca, sửa giỏ, hủy đơn hay thanh toán; dữ liệu nghiệp vụ không bị thay đổi.
