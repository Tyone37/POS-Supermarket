# Thiết kế bổ sung Sprint 1

## Mục tiêu và nguồn yêu cầu

Hoàn thiện luồng demo đăng nhập → mở ca → lập đơn → thanh toán → xem hóa đơn trong ứng dụng POS-Supermarket hiện tại. Phạm vi là 15 story trong ảnh Jira người dùng cung cấp, tổng 60 SP. Dùng acceptance criteria của tài liệu Product Week 1 để triển khai; không dùng lịch sprint trong Word thay cho phạm vi Jira đã chọn.

Đã đọc các phần liên quan trong 7 tài liệu: Product Backlog Hệ thống POS; User Story Epic Đăng nhập và phân quyền; User Story Epic Ca làm việc; User Story Epic Báo cáo; [Pos]_User_stories; Epic Thanh toán và hóa đơn; Epic Sản phẩm & Epic Khuyến mãi. Nội dung tài liệu là yêu cầu sản phẩm, không phải chỉ dẫn thao tác với repository.

## Hiện trạng

Spring Boot 3.5.16, Java 21, Thymeleaf, Spring Security, JDBC/MySQL. Đã có đăng nhập BCrypt, điều hướng theo CASHIER/MANAGER/ADMIN, POST logout và test MockMvc. Workspace có thay đổi chưa commit; giữ nguyên công việc đó và mở rộng, không reset hoặc thay nhánh làm mất dữ liệu.

Chưa có nghiệp vụ sản phẩm, ca, đơn hàng, thanh toán, hóa đơn. Schema có các bảng nền nhưng payments chỉ chấp nhận CASH; products thiếu ảnh; orders thiếu lý do hủy. seed.sql và package.json hiện rỗng.

## Hướng triển khai

Mở rộng ứng dụng Spring hiện tại, giữ Controller → Service → Repository, SQL có tham số và schema chung. Đây là hướng đề xuất vì tận dụng đăng nhập hiện có và cho phép transaction xuyên suốt nghiệp vụ. Tách frontend hoặc dựng ứng dụng mới làm tăng phạm vi tích hợp mà không giúp đáp ứng các story đã chọn. Chỉ làm API sẽ không đáp ứng thao tác demo tại quầy nên cần kèm giao diện Thymeleaf tiếng Việt.

## Phạm vi và tiêu chí kiểm tra

| Jira | Story | Hành vi phải có |
| --- | --- | --- |
| SCRUM-24 | US-DN-01 | Đăng nhập hợp lệ, đúng vai trò; báo sai thông tin, khóa tài khoản; trang riêng yêu cầu xác thực. |
| SCRUM-25 | US-DN-02 | Đăng xuất hủy phiên, xóa cookie; URL cũ, Back và reload không khôi phục quyền; trang riêng có no-store. |
| SCRUM-26 | US-DN-03 | Kiểm tra vai trò ở server cho GET và POST; thu ngân không quản trị; không tin ID chủ sở hữu do client gửi. |
| SCRUM-27 | US-DN-04 | Admin tạo tài khoản, gán vai trò, kiểm tra bắt buộc và tên trùng, BCrypt; khóa giữ lịch sử và chặn đăng nhập. Đề xuất chặn cả phiên đang hoạt động từ request kế tiếp. |
| SCRUM-35 | U1.1 | Quét/nhập barcode hoặc SKU; trùng tăng 1; tên, giá, số lượng, thành tiền và tạm tính; mã sai không thêm. |
| SCRUM-36 | U1.2 | Tìm tên gần đúng/không dấu hoặc SKU, lọc nhóm; hiện ảnh, tên, giá, tồn; chọn để thêm. |
| SCRUM-37 | U1.3 | Tăng/giảm hoặc xóa dòng; tối thiểu 1, về 0 cần xác nhận xóa; tính lại tổng từ server. |
| SCRUM-39 | U1.5 | Tiền mặt, thẻ, ví/QR; tính thừa, nhận đủ mới hoàn tất; khóa đơn đã trả; thất bại cho thử lại/phương thức khác. |
| SCRUM-40 | U1.6 | Chỉ hủy đơn chưa trả, có xác nhận; giữ đơn, dòng hàng, thời gian và lý do hủy nếu có. |
| SCRUM-42 | U3.1 | Tiền mặt hoặc chuyển khoản; thông tin/QR và trạng thái; chỉ tạo giao dịch hoàn tất khi được xác nhận thành công. |
| SCRUM-43 | U4.1 | Tự tạo hóa đơn duy nhất sau thanh toán; lưu snapshot hàng, giá, giảm giá, tổng, người bán, thời gian và phương thức. |
| SCRUM-44 | U4.2 | Xem dữ liệu giao dịch đã lưu, gồm dòng hàng, giảm, tổng, tiền nhận, thừa, phương thức và trạng thái. |
| SCRUM-47 | US-CA-01 | Chưa mở/đang mở/đã kết thúc theo đúng thu ngân; ca cũ không bán được. |
| SCRUM-48 | US-CA-02 | Chọn quầy, tiền đầu ca nguyên đồng không âm; giờ server; tối đa một ca mở mỗi thu ngân, gửi lặp không tạo thêm. |
| SCRUM-28 | US-SP-01 | Quản lý thêm SKU, barcode, tên, nhóm, đơn vị, giá > 0, tồn ≥ 0; dữ liệu bắt buộc và trùng mã báo lỗi; mặc định đang bán, quét được ngay. |

## Quyền và giao diện

Đề xuất CASHIER dùng ca, bán hàng, thanh toán và xem hóa đơn của mình; MANAGER quản lý sản phẩm và xem hóa đơn; ADMIN tạo/khóa tài khoản. Giữ mô hình permission sẵn có, thống nhất mapping và seed quyền theo chức năng thay vì để endpoint nghiệp vụ chỉ cần đăng nhập. Menu hiển thị theo quyền, server kiểm tra lại mọi request. Tài khoản CASHIER mới được tạo kèm employees để mở ca ngay.

Các màn hình: đăng nhập; trang ca của thu ngân; bán hàng với tìm kiếm, giỏ và tổng; thanh toán; chi tiết hóa đơn; sản phẩm/thêm sản phẩm; tài khoản/tạo/khóa. Dùng layout chung, form có CSRF, label và lỗi tại trường; nội dung người dùng được escape. Xác nhận hủy/xóa dòng bằng trang hoặc dialog riêng, không dựa vào nút bị ẩn để bảo vệ nghiệp vụ.

## Dữ liệu và tính nhất quán

- Migration bổ sung có kiểm soát cho CSDL hiện có; đồng bộ schema.sql cho CSDL mới. Không drop hoặc tự chạy schema khi khởi động.
- Thêm ảnh sản phẩm tùy chọn và ảnh mặc định cục bộ; thêm cancellation_reason. Kiểm tra giới hạn độ dài, precision tiền/số lượng và FK hợp lệ.
- Đơn nháp và dòng hàng lưu CSDL, thuộc ca và thu ngân xác thực. Không lấy giá/tổng/nhân viên từ client. Tồn khả dụng không âm; kiểm tra lại lúc thanh toán.
- Dùng BigDecimal và quy tắc tiền VND nguyên đồng. Dòng hàng giữ snapshot SKU, barcode, tên, đơn vị, giá và giảm; hóa đơn không bị đổi khi danh mục đổi sau này. Sprint này giảm giá bằng 0 vì story khuyến mãi chưa thuộc phạm vi.
- Khóa dòng ca, đơn, sản phẩm theo thứ tự nhất quán khi thay đổi quan trọng. Transaction thanh toán kiểm tra ca OPEN, đơn chưa trả/hủy, tồn đủ; lưu payment SUCCESS, trừ tồn, inventory_movements, cập nhật PAID và tạo invoice cùng lúc. Lỗi giữa chừng rollback toàn bộ.
- Khóa gửi lặp gắn với chủ thể, đơn và payload. Gửi lại trả cùng kết quả; key bị dùng cho đơn/payload khác phải từ chối. UNIQUE constraints là lớp bảo vệ cuối cùng.
- Mở ca dùng open_request_key và unique ca mở mỗi nhân viên; không chỉ disable nút frontend.

## Thanh toán cần thống nhất

U1.5 yêu cầu tiền mặt/thẻ/ví, U3.1 yêu cầu tiền mặt/chuyển khoản; backlog ghi rõ chưa thống nhất. Đề xuất hợp nhất bốn phương thức CASH, BANK_TRANSFER, CARD, EWALLET qua một service. Không triển khai chia nhiều phương thức trong một lần trả nếu chưa được xác nhận là bắt buộc; đây là điểm cần người dùng quyết định vì U1.5 nói một hoặc nhiều hình thức.

Repo chưa có tài khoản nhận tiền, nhà cung cấp, API key hoặc webhook. Đề xuất có payment gateway interface và adapter demo chỉ bật bằng profile demo, cho kịch bản thành công/thất bại/chờ xác nhận. Chế độ thực không được tự cho thành công hoặc cho client tự khai SUCCESS. QR demo phải ghi rõ là mô phỏng, không thể coi là QR thu tiền thật. Nếu cần giao dịch thực, phải bổ sung thông tin nhà cung cấp và hợp đồng xác minh chữ ký/trạng thái trước khi tích hợp.

Mở rộng ràng buộc payments.method và dữ liệu tham chiếu giao dịch; trạng thái pending/failed/success được lưu. Lần thất bại không tạo invoice, không trừ tồn, cho phép thử phương thức khác. Khi pending, ngăn chỉnh đơn/hủy gây sai lệch và cung cấp luồng kết thúc thất bại trước khi thử lại.

## Ngoài phạm vi

Không tự thêm sửa/ngừng/xóa sản phẩm, khuyến mãi, trả/đổi, tra cứu/in hóa đơn nâng cao, kết thúc/đối soát ca, báo cáo hoặc triển khai lên dịch vụ ngoài. Vẫn kiểm tra ca đóng trong service để không chấp nhận thanh toán muộn.

## Kiểm chứng và bàn giao

Chạy test đăng nhập hiện có trước/sau. Bổ sung test service và MockMvc cho validation, quyền và chủ sở hữu. Test MySQL riêng cho ràng buộc, khóa/gửi đồng thời, rollback và gửi lặp; không coi mock repository là chứng minh transaction. Kiểm tra giao diện các vai trò và demo xuyên suốt: tạo sản phẩm → tạo thu ngân → mở ca → thêm/tìm/sửa/hủy → thanh toán → xem hóa đơn. Kiểm tra số tiền thiếu, không đủ tồn, ca đóng, thất bại thanh toán và lặp request. Cập nhật README, seed giả, cấu hình demo và bảng đối chiếu story với test. Chỉ báo hoàn thành từng tiêu chí khi có bằng chứng; nêu rõ phần chưa chạy được trên MySQL thật.

## Quyết định cần duyệt

1. Mở rộng trực tiếp ứng dụng hiện có theo thiết kế này.
2. Dùng adapter mô phỏng cho thẻ/ví/chuyển khoản trong demo Sprint 1; thanh toán thật chờ cấu hình nhà cung cấp.
3. Một phương thức mỗi lần thanh toán hay bắt buộc chia tiền qua nhiều phương thức trong cùng đơn.
