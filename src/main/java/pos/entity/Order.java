package pos.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

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

@Entity(name = "SaleOrder")
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 30)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12, columnDefinition = "varchar(12)")
    private OrderStatus status;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant createdAt;

    @Column(name = "paid_at", columnDefinition = "datetime(3)")
    private Instant paidAt;

    @Column(name = "cancelled_at", columnDefinition = "datetime(3)")
    private Instant cancelledAt;

    protected Order() {
    }

    public Order(String orderNo, Shift shift, Instant now) {
        this.orderNo = orderNo;
        this.shift = shift;
        this.status = OrderStatus.DRAFT;
        this.subtotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = now;
    }

    public void cancel(Instant now) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể hủy đơn nháp.");
        }
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
    }

    public void recalculate(List<OrderItem> items) {
        subtotalAmount = BigDecimal.ZERO;
        discountAmount = BigDecimal.ZERO;
        totalAmount = BigDecimal.ZERO;
        for (OrderItem item : items) {
            subtotalAmount = subtotalAmount.add(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
            discountAmount = discountAmount.add(item.getDiscountAmount());
            totalAmount = totalAmount.add(item.getLineTotal());
        }
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Shift getShift() { return shift; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
