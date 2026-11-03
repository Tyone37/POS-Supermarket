package pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddItemRequest(
        @NotBlank(message = "Vui lòng nhập mã vạch hoặc SKU")
        @Size(max = 50, message = "Mã không được quá 50 ký tự") String code) {
    public AddItemRequest {
        code = code == null ? null : code.strip();
    }
}
