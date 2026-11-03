package pos.dto;

import java.math.BigDecimal;

public record ProductSearchRow(Long id, String code, String name, String unitName,
        BigDecimal salePrice, BigDecimal stockQuantity) { }
