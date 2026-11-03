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
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 12)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private PaymentStatus status;

    @Column(name = "amount_due", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_received", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "change_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant createdAt;

    @Column(name = "completed_at", columnDefinition = "datetime(3)")
    private Instant completedAt;

    protected Payment() {
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
