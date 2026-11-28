package pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CouponForm(
        @NotBlank(message = "Vui lòng nhập mã coupon.") @Size(max = 30) String code,
        @NotBlank(message = "Vui lòng nhập tên coupon.") @Size(max = 200) String name,
        @NotBlank @Pattern(regexp = "ORDER|PRODUCT", message = "Phạm vi không hợp lệ.") String scope,
        @Positive Long productId,
        @NotBlank @Pattern(regexp = "PERCENT|AMOUNT", message = "Loại giảm giá không hợp lệ.") String type,
        @NotBlank(message = "Vui lòng nhập mức giảm.") String value,
        @NotBlank(message = "Vui lòng nhập giá trị đơn tối thiểu.") String minimum,
        @NotBlank(message = "Vui lòng nhập thời gian bắt đầu.") String startsAt,
        @NotBlank(message = "Vui lòng nhập thời gian kết thúc.") String endsAt,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED", message = "Trạng thái không hợp lệ.") String status) {
}
