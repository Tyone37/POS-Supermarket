package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "promotion_products")
@IdClass(PromotionProductId.class)
public class PromotionProductEntity {
    @Id
    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    public PromotionProductEntity() {}

    public PromotionProductEntity(Long promotionId, Long productId) {
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
}
