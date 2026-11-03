package pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceDetail(Long id, String invoiceNo, String issuedAt, String issuerName,
        String registerName, String status, String paymentMethod, String paymentStatus,
        BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal totalAmount,
        BigDecimal amountDue, BigDecimal amountReceived, BigDecimal changeAmount,
        List<InvoiceLineView> lines) {
}
