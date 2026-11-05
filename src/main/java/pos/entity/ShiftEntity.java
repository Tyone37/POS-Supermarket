package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "shifts")
public class ShiftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "shift_no", nullable = false, length = 30)
    private String shiftNo;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "register_id", nullable = false)
    private Long registerId;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "open_request_key", nullable = false, length = 64)
    private String openRequestKey;

    @Column(name = "opened_at", nullable = false, columnDefinition = "datetime(3)")
    private Timestamp openedAt;

    @Column(name = "closed_at", columnDefinition = "datetime(3)")
    private Timestamp closedAt;

    @Column(name = "opening_cash", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCash;

    public ShiftEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShiftNo() {
        return shiftNo;
    }

    public void setShiftNo(String shiftNo) {
        this.shiftNo = shiftNo;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getRegisterId() {
        return registerId;
    }

    public void setRegisterId(Long registerId) {
        this.registerId = registerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOpenRequestKey() {
        return openRequestKey;
    }

    public void setOpenRequestKey(String openRequestKey) {
        this.openRequestKey = openRequestKey;
    }

    public Timestamp getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Timestamp openedAt) {
        this.openedAt = openedAt;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }

}
