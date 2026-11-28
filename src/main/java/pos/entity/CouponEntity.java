package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "coupons")
public class CouponEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "discount_type", nullable = false, length = 10)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "starts_at", nullable = false, columnDefinition = "datetime(3)")
    private Timestamp startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "datetime(3)")
    private Timestamp endsAt;

    @Column(name = "lifecycle_status", nullable = false, length = 10)
    private String lifecycleStatus;

    @Column(name = "scope", nullable = false, length = 10)
    private String scope = "ORDER";

    @Column(name = "product_id")
    private Long productId;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public Timestamp getStartsAt() { return startsAt; }
    public void setStartsAt(Timestamp startsAt) { this.startsAt = startsAt; }
    public Timestamp getEndsAt() { return endsAt; }
    public void setEndsAt(Timestamp endsAt) { this.endsAt = endsAt; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
}
