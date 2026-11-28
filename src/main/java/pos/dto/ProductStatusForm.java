package pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductStatusForm {
    @NotNull(message = "Vui lòng chọn trạng thái bán hợp lệ.")
    private Status status;
    @NotBlank(message = "Vui lòng tải lại danh sách sản phẩm trước khi đổi trạng thái.")
    private String expectedUpdatedAt;

    public enum Status {
        ACTIVE, STOPPED
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getExpectedUpdatedAt() { return expectedUpdatedAt; }
    public void setExpectedUpdatedAt(String expectedUpdatedAt) { this.expectedUpdatedAt = expectedUpdatedAt; }
}
