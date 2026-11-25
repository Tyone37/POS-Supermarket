package pos.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class InvoiceSearch {
    private InvoiceSearch() {
    }

    public static class Filter {
        @Size(max = 30, message = "Mã hóa đơn tối đa 30 ký tự.")
        private String code = "";
        @Pattern(regexp = "|CASH|BANK_TRANSFER", message = "Phương thức thanh toán không hợp lệ.")
        private String method = "";
        @Pattern(regexp = "|ISSUED|VOID", message = "Trạng thái hóa đơn không hợp lệ.")
        private String status = "";

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public record Query(String code, String method, String status, Long issuedBy, int offset) {
    }

    public record Row(long id, String invoiceNo, Timestamp issuedAt, String cashierName,
                      String method, String status, BigDecimal totalAmount) {
    }

    public record Result(List<Row> items, int page, boolean hasNext) {
        public Result {
            items = List.copyOf(items);
        }
    }
}
