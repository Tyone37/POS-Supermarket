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
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 64)
    private String eventKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_code_snapshot", nullable = false, length = 20)
    private String productCodeSnapshot;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10, columnDefinition = "varchar(10)")
    private MovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityDelta;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(Product product, Instant now) {
        this.eventKey = "PRODUCT_CREATE:" + product.getCode();
        this.product = product;
        this.productCodeSnapshot = product.getCode();
        this.movementType = MovementType.ADJUSTMENT;
        this.quantityDelta = product.getStockQuantity();
        this.reason = "Tồn đầu kỳ khi tạo sản phẩm";
        this.createdAt = now;
    }
}
