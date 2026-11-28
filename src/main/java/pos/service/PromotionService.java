package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class PromotionService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final PosRepository db;

    public PromotionService(PosRepository db) {
        this.db = db;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String actor) {
        PosSupport.actor(db, actor, "MANAGER", false);
        Timestamp now = PosSupport.now();
        return db.promotions().stream().map(row -> enrich(row, now, false)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String actor, long id) {
        PosSupport.actor(db, actor, "MANAGER", false);
        var promo = db.promotion(id, false)
                .orElseThrow(() -> new BusinessException("Chương trình khuyến mãi không tồn tại."));
        return enrich(promo, PosSupport.now(), true);
    }

    @Transactional
    public long create(String actor, Forms.Promotion form) {
        PosSupport.actor(db, actor, "MANAGER");
        Timestamp now = PosSupport.now();
        String code = form.code() == null || form.code().isBlank()
                ? "KM" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT)
                : PosSupport.text(form.code(), "Mã khuyến mãi", 20).toUpperCase(Locale.ROOT);
        if (db.promotions().stream().anyMatch(p -> code.equalsIgnoreCase(String.valueOf(p.get("code"))))) {
            throw new BusinessException("Mã khuyến mãi đã tồn tại.");
        }
        ValidatedPromotion valid = validateAndParse(form, now, null, false);
        return db.createPromotion(code, valid.name(), valid.discountPercent(),
                valid.startsAt(), valid.endsAt(), valid.productIds());
    }

    @Transactional
    public void update(String actor, long id, Forms.Promotion form) {
        PosSupport.actor(db, actor, "MANAGER");
        var promo = db.promotion(id, true)
                .orElseThrow(() -> new BusinessException("Chương trình khuyến mãi không tồn tại."));
        Timestamp now = PosSupport.now();
        String status = statusOf(promo, now);
        if ("ACTIVE".equals(status)) {
            throw new BusinessException("Chương trình đang áp dụng chỉ được kết thúc sớm, không thể chỉnh sửa.");
        }
        if (!"UPCOMING".equals(status)) {
            throw new BusinessException("Chương trình đã kết thúc hoặc đã hủy, chỉ được xem và không thể chỉnh sửa.");
        }
        ValidatedPromotion valid = validateAndParse(form, now, id, true);
        db.updatePromotion(id, valid.name(), valid.discountPercent(),
                valid.startsAt(), valid.endsAt(), valid.productIds());
    }

    @Transactional
    public void cancel(String actor, long id, boolean confirmed) {
        PosSupport.actor(db, actor, "MANAGER");
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận hủy / kết thúc sớm chương trình khuyến mãi.");
        }
        var promo = db.promotion(id, true)
                .orElseThrow(() -> new BusinessException("Chương trình khuyến mãi không tồn tại."));
        Timestamp now = PosSupport.now();
        String status = statusOf(promo, now);
        if ("ENDED".equals(status) || "CANCELED".equals(status)) {
            throw new BusinessException("Chương trình đã kết thúc hoặc đã hủy trước đó.");
        }
        db.cancelPromotion(id, now);
    }

    public static String statusOf(Map<String, Object> promo, Timestamp now) {
        if ("CANCELED".equals(promo.get("lifecycle_status"))) {
            return "CANCELED";
        }
        Timestamp startsAt = (Timestamp) promo.get("starts_at");
        Timestamp endsAt = (Timestamp) promo.get("ends_at");
        if (now.before(startsAt)) {
            return "UPCOMING";
        }
        if (!now.before(endsAt)) {
            return "ENDED";
        }
        return "ACTIVE";
    }

    private Map<String, Object> enrich(Map<String, Object> promo, Timestamp now, boolean includeProducts) {
        Map<String, Object> row = new LinkedHashMap<>(promo);
        row.put("effective_status", statusOf(promo, now));
        List<Long> productIds = db.promotionProductIds(PosSupport.id(promo, "id"));
        row.put("product_count", productIds.size());
        if (includeProducts) {
            row.put("product_ids", productIds);
        }
        return row;
    }

    private ValidatedPromotion validateAndParse(Forms.Promotion form, Timestamp now, Long excludePromotionId,
                                                boolean requireFutureStart) {
        String name = PosSupport.text(form.name(), "Tên chương trình", 200);
        BigDecimal discountPercent = parseDiscountPercent(form.discountPercent());
        Timestamp startsAt = parseDateTime(form.startsAt(), "Thời gian bắt đầu");
        Timestamp endsAt = parseDateTime(form.endsAt(), "Thời gian kết thúc");

        if (!startsAt.before(endsAt)) {
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (requireFutureStart && startsAt.before(now)) {
            throw new BusinessException("Thời gian bắt đầu phải từ hiện tại trở đi.");
        }

        List<Long> rawIds = form.productIds();
        List<Long> productIds = rawIds == null ? List.of()
                : rawIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (productIds.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một sản phẩm áp dụng.");
        }

        for (Long productId : productIds) {
            db.product(productId, false)
                    .orElseThrow(() -> new BusinessException("Sản phẩm áp dụng không tồn tại."));
            var conflicts = db.overlappingPromotions(productId, startsAt, endsAt, excludePromotionId);
            if (!conflicts.isEmpty()) {
                throw new BusinessException("Sản phẩm bị trùng thời gian với chương trình khuyến mãi khác ("
                        + conflicts.getFirst().get("code") + ").");
            }
        }
        return new ValidatedPromotion(name, discountPercent, startsAt, endsAt, productIds);
    }

    private BigDecimal parseDiscountPercent(String input) {
        try {
            BigDecimal value = new BigDecimal(input == null ? "" : input.strip());
            if (value.signum() <= 0 || value.compareTo(new BigDecimal("100")) > 0
                    || value.stripTrailingZeros().scale() > 2) {
                throw new NumberFormatException();
            }
            return value.setScale(2);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new BusinessException("Phần trăm giảm giá phải lớn hơn 0, tối đa 100 và tối đa 2 chữ số thập phân.");
        }
    }

    private Timestamp parseDateTime(String input, String label) {
        String text = input == null ? "" : input.strip();
        if (text.isEmpty()) {
            throw new BusinessException(label + " bắt buộc nhập.");
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(text);
            return Timestamp.from(ldt.atZone(ZONE).toInstant());
        } catch (Exception ex) {
            throw new BusinessException(label + " không đúng định dạng ngày giờ.");
        }
    }

    private record ValidatedPromotion(String name, BigDecimal discountPercent, Timestamp startsAt, Timestamp endsAt,
                                      List<Long> productIds) { }
}
