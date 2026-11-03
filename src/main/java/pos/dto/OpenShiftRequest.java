package pos.dto;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record OpenShiftRequest(
        @NotNull(message = "Vui lòng chọn quầy") @Positive(message = "Quầy không hợp lệ") Long registerId,
        @NotNull(message = "Vui lòng nhập tiền đầu ca")
        @DecimalMin(value = "0", message = "Tiền đầu ca phải lớn hơn hoặc bằng 0")
        @Digits(integer = 16, fraction = 2, message = "Tiền đầu ca tối đa 16 chữ số nguyên và 2 chữ số lẻ") BigDecimal openingCash,
        @NotBlank(message = "Thiếu khóa yêu cầu. Vui lòng mở lại form")
        @Pattern(regexp = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                message = "Khóa yêu cầu không hợp lệ. Vui lòng mở lại form") String requestKey) {
    public OpenShiftRequest {
        requestKey = requestKey == null ? null : requestKey.strip().toLowerCase(Locale.ROOT);
    }
}
