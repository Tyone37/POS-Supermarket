package pos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class ShiftService {
    private final PosRepository db;
    public ShiftService(PosRepository db) { this.db = db; }

    @Transactional
    public Optional<Map<String, Object>> current(String username) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        return db.latestShift(employeeId(PosSupport.id(actor, "id")));
    }

    public List<Map<String, Object>> registers() { return db.registers(); }

    long employeeId(long account) {
        return PosSupport.id(db.employee(account)
                .orElseThrow(() -> new BusinessException("Tài khoản chưa có hồ sơ nhân viên đang hoạt động.")), "id");
    }

    @Transactional
    public long open(String username, Long register, String openingCash, String key) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        long employee = employeeId(PosSupport.id(actor, "id"));
        BigDecimal cash = PosSupport.money(openingCash, "Tiền đầu ca");
        String request = PosSupport.requestKey(key);
        if (register == null || db.registers().stream().noneMatch(row -> PosSupport.id(row, "id") == register)) {
            throw new BusinessException("Vui lòng chọn quầy đang hoạt động.");
        }
        var replay = db.shiftByKey(request);
        if (replay.isPresent()) {
            var shift = replay.get();
            if (PosSupport.id(shift, "employee_id") != employee || PosSupport.id(shift, "register_id") != register
                    || ((BigDecimal) shift.get("opening_cash")).compareTo(cash) != 0) {
                throw new BusinessException("Mã mở ca đã được dùng cho yêu cầu khác.");
            }
            return PosSupport.id(shift, "id");
        }
        var latest = db.latestShift(employee);
        if (latest.isPresent() && "OPEN".equals(latest.get().get("status"))) {
            return PosSupport.id(latest.get(), "id");
        }
        return db.createShift(PosSupport.number("CA-"), employee, register, cash, request, PosSupport.now());
    }
}
