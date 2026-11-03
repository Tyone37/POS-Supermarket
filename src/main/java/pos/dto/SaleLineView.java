package pos.dto;

import java.math.BigDecimal;

public record SaleLineView(Long id, Long productId, String productCode, String productName,
        String barcode, String unitCode, BigDecimal quantity, BigDecimal unitPrice,
        BigDecimal discountAmount, BigDecimal lineTotal) { }
