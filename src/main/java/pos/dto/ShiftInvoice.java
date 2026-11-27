package pos.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Saved invoice data used by the current-shift summary. */
public record ShiftInvoice(Long id, String invoiceNo, String cashierName, Timestamp issuedAt,
                           BigDecimal totalAmount, String method) {
}
