package pos.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refund_payments")
public class RefundPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "return_receipt_id", nullable = false)
    private Long returnReceiptId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "method", nullable = false, length = 16)
    private String method;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Timestamp createdAt;

    @Column(name = "completed_at", columnDefinition = "datetime(3)")
    private Timestamp completedAt;
}
