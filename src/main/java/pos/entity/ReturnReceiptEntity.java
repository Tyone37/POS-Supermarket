package pos.entity;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Accounting reference for returns. The original invoice is never rewritten. */
@Entity
@Table(name = "return_receipts")
public class ReturnReceiptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "return_no", nullable = false, length = 30)
    private String returnNo;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "status", nullable = false, length = 12)
    private String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(3)")
    private Timestamp createdAt;

    @Column(name = "completed_at", columnDefinition = "datetime(3)")
    private Timestamp completedAt;
}
