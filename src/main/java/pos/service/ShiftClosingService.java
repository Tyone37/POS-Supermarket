package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.ShiftClosePreview;
import pos.dto.ShiftInvoice;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class ShiftClosingService {
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999999.99");
    private final PosRepository db;
    private final Clock clock;

    public ShiftClosingService(PosRepository db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    @Transactional
    public ShiftClosePreview preview(String username, String actualCash) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        long accountId = PosSupport.id(actor, "id");
        return calculate(openShift(accountId), accountId, PosSupport.money(actualCash, "Tiền thực tế"));
    }

    @Transactional
    public void close(String username, ShiftClosePreview reviewed) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        long accountId = PosSupport.id(actor, "id");
        var shift = openShift(accountId);
        if (reviewed == null || reviewed.shiftId() != PosSupport.id(shift, "id")) {
            throw new BusinessException("Ca cần kết thúc đã thay đổi. Vui lòng xem lại số liệu.");
        }
        var current = calculate(shift, accountId,
                PosSupport.money(reviewed.actualCash().toPlainString(), "Tiền thực tế"));
        if (!sameFigures(reviewed, current)) {
            throw new BusinessException("Số liệu ca đã thay đổi. Vui lòng xem và xác nhận lại số liệu mới.");
        }
        var time = Timestamp.from(clock.instant().truncatedTo(ChronoUnit.MILLIS));
        if (time.before(current.openedAt())) {
            throw new BusinessException("Giờ kết thúc không được trước giờ mở ca. Vui lòng kiểm tra giờ hệ thống.");
        }
        db.closeShift(current, time);
    }

    private Map<String, Object> openShift(long accountId) {
        var employee = db.employee(accountId)
                .orElseThrow(() -> new BusinessException("Tài khoản chưa có hồ sơ nhân viên đang hoạt động."));
        var latest = db.latestShift(PosSupport.id(employee, "id"))
                .orElseThrow(() -> new BusinessException("Không có ca đang mở để kết thúc."));
        // Checkout also locks this account first. Refresh the shift after acquiring its database lock.
        var shift = db.shift(PosSupport.id(latest, "id"))
                .orElseThrow(() -> new BusinessException("Ca không tồn tại."));
        if (PosSupport.id(shift, "account_id") != accountId) {
            throw new AccessDeniedException("Không được kết thúc ca của thu ngân khác.");
        }
        if (!"OPEN".equals(shift.get("status"))) {
            throw new BusinessException("Không có ca đang mở để kết thúc.");
        }
        return shift;
    }

    private ShiftClosePreview calculate(Map<String, Object> shift, long accountId, BigDecimal actualCash) {
        long shiftId = PosSupport.id(shift, "id");
        var invoices = db.completedShiftInvoices(shiftId, accountId);
        BigDecimal revenue = invoices.stream().map(ShiftInvoice::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashSales = invoices.stream().filter(invoice -> "CASH".equals(invoice.method()))
                .map(ShiftInvoice::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = db.completedCashRefunds(shiftId, accountId);
        BigDecimal opening = (BigDecimal) shift.get("opening_cash");
        BigDecimal expected = opening.add(cashSales).subtract(refunds);
        BigDecimal difference = actualCash.subtract(expected);
        if (revenue.compareTo(MAX_MONEY) > 0 || cashSales.compareTo(MAX_MONEY) > 0
                || refunds.compareTo(MAX_MONEY) > 0 || expected.compareTo(MAX_MONEY) > 0
                || difference.abs().compareTo(MAX_MONEY) > 0) {
            throw new BusinessException("Số liệu đối soát vượt giới hạn lưu trữ của ca.");
        }
        if (refunds.signum() < 0 || expected.signum() < 0) {
            throw new BusinessException("Số liệu hoàn tiền không hợp lệ: tiền mặt dự kiến không được âm.");
        }
        return new ShiftClosePreview(shiftId, (String) shift.get("shift_no"), (Timestamp) shift.get("opened_at"),
                opening, invoices.size(), revenue, cashSales, refunds, expected, actualCash, difference);
    }

    private boolean sameFigures(ShiftClosePreview reviewed, ShiftClosePreview current) {
        return reviewed.invoiceCount() == current.invoiceCount()
                && reviewed.openingCash().compareTo(current.openingCash()) == 0
                && reviewed.revenue().compareTo(current.revenue()) == 0
                && reviewed.cashSales().compareTo(current.cashSales()) == 0
                && reviewed.cashRefunds().compareTo(current.cashRefunds()) == 0
                && reviewed.expectedCash().compareTo(current.expectedCash()) == 0;
    }
}
