package pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleView(Long id, String orderNo, String status, Long shiftId, String registerName,
        String actorName, List<SaleLineView> lines, BigDecimal subtotalAmount,
        BigDecimal discountAmount, BigDecimal totalAmount, String createdAt) { }
