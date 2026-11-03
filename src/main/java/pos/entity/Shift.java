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
@Table(name = "shifts")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_no", nullable = false, length = 30)
    private String shiftNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "register_id", nullable = false)
    private Register register;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10)")
    private ShiftStatus status;

    @Column(name = "open_request_key", nullable = false, length = 64)
    private String openRequestKey;

    @Column(name = "opened_at", nullable = false, columnDefinition = "datetime(3)")
    private Instant openedAt;

    @Column(name = "closed_at", columnDefinition = "datetime(3)")
    private Instant closedAt;

    @Column(name = "opening_cash", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCash;

    protected Shift() {
    }

    public Long getId() {
        return id;
    }

    public String getShiftNo() {
        return shiftNo;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Register getRegister() {
        return register;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public String getOpenRequestKey() {
        return openRequestKey;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public BigDecimal getOpeningCash() {
        return openingCash;
    }
}
