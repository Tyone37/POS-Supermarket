package pos.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** Historical invoice values only; printing never reads current product prices. */
public record InvoicePrint(long id, String invoiceNo, Timestamp issuedAt, String cashierName, String method,
                           String status, String bankReference, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal total, BigDecimal received, BigDecimal change, List<Line> items) {
    public InvoicePrint {
        items = List.copyOf(items);
    }

    public enum Paper {
        THERMAL
    }

    public record Line(String productCode, String productName, String unitCode, BigDecimal quantity,
                       BigDecimal unitPrice, BigDecimal discount, BigDecimal total) {
    }
}
