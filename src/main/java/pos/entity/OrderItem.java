package pos.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code_snapshot", nullable = false, length = 20)
    private String productCodeSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot;

    @Column(name = "barcode_snapshot", nullable = false, length = 50)
    private String barcodeSnapshot;

    @Column(name = "unit_code_snapshot", nullable = false, length = 12)
    private String unitCodeSnapshot;

    @Column(nullable = false, precision = 15, scale = 3)
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

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, BigDecimal quantity) {
        this.order = order;
        this.product = product;
        this.productCodeSnapshot = product.getCode();
        this.productNameSnapshot = product.getName();
        this.barcodeSnapshot = product.getBarcode();
        this.unitCodeSnapshot = product.getUnit().getCode();
        this.unitPrice = product.getSalePrice();
        this.discountAmount = BigDecimal.ZERO;
        changeQuantity(quantity);
    }

    public void changeQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.lineTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP).subtract(discountAmount);
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Product getProduct() { return product; }
    public String getProductCodeSnapshot() { return productCodeSnapshot; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public String getBarcodeSnapshot() { return barcodeSnapshot; }
    public String getUnitCodeSnapshot() { return unitCodeSnapshot; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Long getPromotionId() { return promotionId; }
    public String getPromotionCodeSnapshot() { return promotionCodeSnapshot; }
    public String getPromotionNameSnapshot() { return promotionNameSnapshot; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
