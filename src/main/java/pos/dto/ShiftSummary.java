package pos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** The latest shift and completed sales of its open period; shift is null before the first opening. */
public record ShiftSummary(Map<String, Object> shift, List<ShiftInvoice> invoices,
                           BigDecimal revenue, BigDecimal cashRevenue) {
    public ShiftSummary {
        invoices = List.copyOf(invoices);
    }

    public int invoiceCount() {
        return invoices.size();
    }
}
