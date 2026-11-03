package pos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank(message = "Vui lòng nhập mã vạch")
        @Size(max = 50, message = "Mã vạch không được quá 50 ký tự") String barcode,
        @NotBlank(message = "Vui lòng nhập tên sản phẩm")
        @Size(max = 200, message = "Tên sản phẩm không được quá 200 ký tự") String name,
        @Positive(message = "Loại hàng không hợp lệ") Long categoryId,
        @Positive(message = "Đơn vị không hợp lệ") Long unitId,
        @NotNull(message = "Vui lòng nhập đơn giá")
        @DecimalMin(value = "0", inclusive = false, message = "Đơn giá phải lớn hơn 0")
        @Digits(integer = 16, fraction = 2, message = "Đơn giá tối đa 16 chữ số nguyên và 2 chữ số lẻ") BigDecimal salePrice,
        @NotNull(message = "Vui lòng nhập số lượng")
        @DecimalMin(value = "0", message = "Số lượng phải lớn hơn hoặc bằng 0")
        @Digits(integer = 12, fraction = 3, message = "Số lượng tối đa 12 chữ số nguyên và 3 chữ số lẻ")
        BigDecimal stockQuantity) {

    public CreateProductRequest {
        barcode = barcode == null ? null : barcode.strip();
        name = name == null ? null : name.strip();
    }
}
