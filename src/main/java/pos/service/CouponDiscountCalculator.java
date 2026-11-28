package pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pos.exception.BusinessException;

/** Computes a discount plan without changing orders, payments or historical invoices. */
final class CouponDiscountCalculator {
    private CouponDiscountCalculator() { }

    record Allocation(Map<String, Object> item, BigDecimal discount, BigDecimal total) { }

    static List<Allocation> plan(Map<String, Object> coupon, List<Map<String, Object>> items, Timestamp now) {
        if (!"ENABLED".equals(coupon.get("lifecycle_status"))
                || now.before((Timestamp) coupon.get("starts_at")) || !now.before((Timestamp) coupon.get("ends_at"))) {
            throw new BusinessException("Mã coupon chưa đến hạn, đã tắt hoặc đã hết hiệu lực.");
        }
        BigDecimal subtotal = items.stream().map(CouponDiscountCalculator::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal minimum = (BigDecimal) coupon.get("min_order_amount");
        if (minimum != null && subtotal.compareTo(minimum) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng coupon.");
        }
        String scope = coupon.get("scope") == null ? "ORDER" : coupon.get("scope").toString();
        if (!List.of("ORDER", "PRODUCT").contains(scope)) {
            throw new BusinessException("Phạm vi coupon không hợp lệ.");
        }
        Long product = coupon.get("product_id") == null ? null : ((Number) coupon.get("product_id")).longValue();
        var eligible = items.stream().filter(item -> "ORDER".equals(scope)
                || (product != null && PosSupport.id(item, "product_id") == product)).toList();
        BigDecimal base = eligible.stream().map(CouponDiscountCalculator::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (base.signum() <= 0) {
            throw new BusinessException("Giỏ hàng chưa có sản phẩm được áp dụng coupon này.");
        }
        BigDecimal value = (BigDecimal) coupon.get("discount_value");
        String type = (String) coupon.get("discount_type");
        if (value == null || value.signum() <= 0 || (!"PERCENT".equals(type) && !"AMOUNT".equals(type))
                || ("PERCENT".equals(type) && value.compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException("Giá trị coupon không hợp lệ.");
        }
        BigDecimal total = "PERCENT".equals(type)
                ? base.multiply(value).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).setScale(2)
                : value.setScale(0, RoundingMode.HALF_UP).setScale(2);
        if (total.signum() <= 0 || total.compareTo(base) > 0) {
            throw new BusinessException("Giá trị giảm của coupon không hợp lệ hoặc vượt giá trị được áp dụng.");
        }
        return allocate(items, eligible, total, base);
    }

    static List<Allocation> orderPlan(List<Map<String, Object>> items, BigDecimal total) {
        return allocate(items, items, total,
                items.stream().map(CouponDiscountCalculator::gross).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static List<Allocation> allocate(List<Map<String, Object>> items, List<Map<String, Object>> eligible,
                                              BigDecimal total, BigDecimal base) {
        BigDecimal remaining = total;
        BigDecimal capacity = base;
        var result = new ArrayList<Allocation>();
        for (var item : items) {
            BigDecimal gross = gross(item);
            BigDecimal discount = BigDecimal.ZERO;
            if (eligible.contains(item)) {
                capacity = capacity.subtract(gross);
                BigDecimal share = total.multiply(gross).divide(base, 0, RoundingMode.HALF_UP).setScale(2);
                // Bound each allocation so the exact remainder always fits the remaining lines.
                discount = share.max(remaining.subtract(capacity).max(BigDecimal.ZERO)).min(gross).min(remaining);
                remaining = remaining.subtract(discount);
            }
            result.add(new Allocation(item, discount, gross.subtract(discount)));
        }
        return List.copyOf(result);
    }

    private static BigDecimal gross(Map<String, Object> item) {
        return ((BigDecimal) item.get("quantity")).multiply((BigDecimal) item.get("unit_price"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
