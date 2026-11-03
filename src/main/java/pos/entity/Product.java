package pos.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "stock_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal stockQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private ProductStatus status;

    @Column(name = "stopped_at", columnDefinition = "datetime(3)")
    private Instant stoppedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String code, String barcode, String name, ProductCategory category, Unit unit,
            BigDecimal salePrice, BigDecimal stockQuantity, Instant now) {
        this.code = code;
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.salePrice = salePrice;
        this.stockQuantity = stockQuantity;
        this.reservedQuantity = BigDecimal.ZERO;
        this.status = ProductStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }

    public String getCode() { return code; }

    public String getBarcode() { return barcode; }

    public String getName() { return name; }

    public Unit getUnit() { return unit; }

    public BigDecimal getSalePrice() { return salePrice; }

    public BigDecimal getStockQuantity() { return stockQuantity; }

    public ProductStatus getStatus() { return status; }
}
