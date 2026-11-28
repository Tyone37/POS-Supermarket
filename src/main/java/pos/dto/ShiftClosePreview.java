package pos.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Server-calculated figures retained for confirmation; never bound from a confirmation request. */
public record ShiftClosePreview(long shiftId, String shiftNo, Timestamp openedAt, BigDecimal openingCash,
                                int invoiceCount, BigDecimal revenue, BigDecimal cashSales, BigDecimal cashRefunds,
                                BigDecimal expectedCash, BigDecimal actualCash, BigDecimal difference) {
}
