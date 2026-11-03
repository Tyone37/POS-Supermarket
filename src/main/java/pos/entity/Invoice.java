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
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 30)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private Account issuedBy;

    @Column(name = "issued_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant issuedAt;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private InvoiceStatus status;

    protected Invoice() {
    }

    public Invoice(String invoiceNo, Order order, Payment payment, Account issuedBy, Instant now) {
        this.invoiceNo = invoiceNo;
        this.order = order;
        this.payment = payment;
        this.issuedBy = issuedBy;
        this.issuedAt = now;
        this.subtotalAmount = order.getSubtotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.totalAmount = order.getTotalAmount();
        this.status = InvoiceStatus.ISSUED;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public Order getOrder() {
        return order;
    }

    public Payment getPayment() {
        return payment;
    }

    public Account getIssuedBy() {
        return issuedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }
}
