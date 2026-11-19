package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code_snapshot", nullable = false, length = 20)
    private String productCodeSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot;

    @Column(name = "barcode_snapshot", nullable = false, length = 50)
    private String barcodeSnapshot;

    @Column(name = "unit_code_snapshot", nullable = false, length = 12)
    private String unitCodeSnapshot;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "promotion_code_snapshot", length = 20)
    private String promotionCodeSnapshot;

    @Column(name = "promotion_name_snapshot", length = 200)
    private String promotionNameSnapshot;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    public OrderItemEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCodeSnapshot() {
        return productCodeSnapshot;
    }

    public void setProductCodeSnapshot(String productCodeSnapshot) {
        this.productCodeSnapshot = productCodeSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getBarcodeSnapshot() {
        return barcodeSnapshot;
    }

    public void setBarcodeSnapshot(String barcodeSnapshot) {
        this.barcodeSnapshot = barcodeSnapshot;
    }

    public String getUnitCodeSnapshot() {
        return unitCodeSnapshot;
    }

    public void setUnitCodeSnapshot(String unitCodeSnapshot) {
        this.unitCodeSnapshot = unitCodeSnapshot;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }

    public String getPromotionCodeSnapshot() {
        return promotionCodeSnapshot;
    }

    public void setPromotionCodeSnapshot(String promotionCodeSnapshot) {
        this.promotionCodeSnapshot = promotionCodeSnapshot;
    }

    public String getPromotionNameSnapshot() {
        return promotionNameSnapshot;
    }

    public void setPromotionNameSnapshot(String promotionNameSnapshot) {
        this.promotionNameSnapshot = promotionNameSnapshot;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
