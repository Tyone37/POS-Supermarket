package pos.entity;

import java.io.Serializable;
import java.util.Objects;

public class PromotionProductId implements Serializable {
    private Long promotionId;
    private Long productId;

    public PromotionProductId() {}

    public PromotionProductId(Long promotionId, Long productId) {
        this.promotionId = promotionId;
        this.productId = productId;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionProductId that = (PromotionProductId) o;
        return Objects.equals(promotionId, that.promotionId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promotionId, productId);
    }
}
