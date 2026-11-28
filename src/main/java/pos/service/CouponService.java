package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.CouponForm;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class CouponService {
    private static final int PAGE_SIZE = 20;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final PosRepository db;

    public CouponService(PosRepository db) { this.db = db; }

    public record Page(List<Map<String, Object>> items, int page, boolean hasNext) {
        public Page { items = List.copyOf(items); }
    }

    @Transactional(readOnly = true)
    public Page list(String username, String q, int page) {
        PosSupport.actor(db, username, "MANAGER", false);
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE || (q != null && q.length() > 200)) {
            throw new BusinessException("Bộ lọc coupon không hợp lệ.");
        }
        var rows = db.coupons(q == null ? "" : q.strip(), page * PAGE_SIZE, PAGE_SIZE + 1);
        var names = db.products(null).stream().collect(Collectors.toMap(p -> PosSupport.id(p, "id"),
                p -> p.get("code") + " — " + p.get("name")));
        var items = rows.stream().limit(PAGE_SIZE).map(row -> {
            var result = new LinkedHashMap<>(row);
            result.put("product_name", row.get("product_id") == null ? "Toàn đơn hàng"
                    : names.getOrDefault(((Number) row.get("product_id")).longValue(), "Sản phẩm không còn khả dụng"));
            return (Map<String, Object>) result;
        }).toList();
        return new Page(items, page, rows.size() > PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String username, long id) {
        PosSupport.actor(db, username, "MANAGER", false);
        return existing(id, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> products(String username) {
        PosSupport.actor(db, username, "MANAGER", false);
        return db.products(null);
    }

    @Transactional
    public long save(String username, Long id, CouponForm form) {
        PosSupport.actor(db, username, "MANAGER");
        String code = PosSupport.text(form.code(), "Mã coupon", 30).toUpperCase(Locale.ROOT);
        if (id != null) {
            var coupon = existing(id, true);
            if (!code.equals(coupon.get("code"))) {
                throw new BusinessException("Không thể đổi mã coupon sau khi tạo.");
            }
        } else if (db.couponByCode(code).isPresent()) {
            throw new BusinessException("Mã coupon đã tồn tại.");
        }
        String name = PosSupport.text(form.name(), "Tên coupon", 200);
        if (!List.of("ORDER", "PRODUCT").contains(form.scope() == null ? "" : form.scope())
                || !List.of("PERCENT", "AMOUNT").contains(form.type() == null ? "" : form.type())
                || !List.of("ENABLED", "DISABLED").contains(form.status() == null ? "" : form.status())) {
            throw new BusinessException("Phạm vi, loại giảm giá hoặc trạng thái coupon không hợp lệ.");
        }
        BigDecimal value;
        if ("AMOUNT".equals(form.type())) {
            value = PosSupport.money(form.value(), "Mức giảm");
        } else {
            try {
                value = new BigDecimal(form.value() == null ? "" : form.value().strip());
                if (value.signum() <= 0 || value.compareTo(new BigDecimal("100")) > 0
                        || value.stripTrailingZeros().scale() > 2) { throw new NumberFormatException(); }
                value = value.setScale(2);
            } catch (NumberFormatException | ArithmeticException exception) {
                throw new BusinessException("Phần trăm giảm phải lớn hơn 0, tối đa 100 và tối đa 2 số lẻ.");
            }
        }
        if (value.signum() <= 0) { throw new BusinessException("Mức giảm phải lớn hơn 0."); }
        BigDecimal minimum = PosSupport.money(form.minimum(), "Giá trị đơn tối thiểu");
        Timestamp start = date(form.startsAt());
        Timestamp end = date(form.endsAt());
        if (!start.before(end)) { throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu."); }
        Long productId = null;
        if ("PRODUCT".equals(form.scope())) {
            if (form.productId() == null || form.productId() <= 0) {
                throw new BusinessException("Vui lòng chọn sản phẩm áp dụng coupon.");
            }
            var product = db.product(form.productId(), true)
                    .orElseThrow(() -> new BusinessException("Sản phẩm áp dụng không tồn tại."));
            if ("ENABLED".equals(form.status()) && !"ACTIVE".equals(product.get("status"))) {
                throw new BusinessException("Sản phẩm áp dụng đã ngừng bán.");
            }
            productId = form.productId();
        }
        return db.saveCoupon(id, code, name, form.type(), value, minimum, start, end,
                form.status(), form.scope(), productId);
    }

    private Map<String, Object> existing(long id, boolean lock) {
        if (id <= 0) { throw new BusinessException("Mã coupon không hợp lệ."); }
        return db.coupon(id, lock).orElseThrow(() -> new BusinessException("Coupon không tồn tại."));
    }

    private Timestamp date(String input) {
        try {
            return Timestamp.from(LocalDateTime.parse(input == null ? "" : input.strip()).atZone(ZONE).toInstant());
        } catch (DateTimeParseException exception) {
            throw new BusinessException("Thời gian coupon không hợp lệ.");
        }
    }
}
