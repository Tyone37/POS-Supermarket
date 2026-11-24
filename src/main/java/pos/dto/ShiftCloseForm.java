package pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ShiftCloseForm {
    @NotBlank(message = "Vui lòng nhập tiền thực tế.")
    @Size(max = 40, message = "Số tiền vượt quá giới hạn cho phép.")
    @Pattern(regexp = "[0-9]{1,16}(?:\\.0{1,2})?",
            message = "Tiền thực tế phải là số nguyên đồng không âm, tối đa 16 chữ số.")
    private String actualCash;

    public String getActualCash() {
        return actualCash;
    }

    public void setActualCash(String actualCash) {
        this.actualCash = actualCash;
    }
}
