package pos.dto;

import java.math.BigDecimal;

import pos.entity.ProductStatus;

public record ProductRow(String code, String barcode, String name, String unitName,
        BigDecimal salePrice, BigDecimal stockQuantity, ProductStatus status) {
}
