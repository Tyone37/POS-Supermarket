package pos.dto;

import java.math.BigDecimal;

public record InvoiceLineView(String productCode, String productName, String barcode, String unitCode,
        BigDecimal quantity, BigDecimal unitPrice, String promotionCode, String promotionName,
        BigDecimal discountAmount, BigDecimal lineTotal) {
}
