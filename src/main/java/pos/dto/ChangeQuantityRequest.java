package pos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ChangeQuantityRequest(
        @NotNull(message = "Vui lòng nhập số lượng")
        @DecimalMin(value = "1", message = "Số lượng phải từ 1 trở lên")
        @Digits(integer = 12, fraction = 3, message = "Số lượng vượt giới hạn cho phép") BigDecimal quantity) {
}
