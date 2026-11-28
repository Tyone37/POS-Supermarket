package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pos.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;

class CouponDiscountCalculatorTest {
    private final Timestamp now = Timestamp.from(Instant.parse("2026-11-28T00:00:00Z"));

    private Map<String, Object> coupon(String scope, String type, String value) {
        var c = new HashMap<String, Object>();
        c.put("scope", scope); c.put("product_id", 1L); c.put("discount_type", type);
        c.put("discount_value", new BigDecimal(value)); c.put("min_order_amount", BigDecimal.ZERO);
        c.put("lifecycle_status", "ENABLED"); c.put("starts_at", now);
        c.put("ends_at", Timestamp.from(now.toInstant().plusSeconds(60)));
        return c;
    }

    private Map<String, Object> item(long id, long product, String qty, String price) {
        return Map.of("id", id, "product_id", product, "quantity", new BigDecimal(qty), "unit_price", new BigDecimal(price));
    }

    private void money(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }

    @Test void wholeOrderPercentIncludesEveryProduct() {
        var p = CouponDiscountCalculator.plan(coupon("ORDER", "PERCENT", "10"),
                List.of(item(1, 1, "2", "30000"), item(2, 2, "1", "40000")), now);
        money("6000", p.get(0).discount()); money("4000", p.get(1).discount()); money("36000", p.get(1).total());
    }

    @Test void productPercentIncludesEntireQuantityAndExcludesOtherProduct() {
        var p = CouponDiscountCalculator.plan(coupon("PRODUCT", "PERCENT", "10"),
                List.of(item(2, 2, "1", "40000"), item(1, 1, "3", "30000")), now);
        money("0", p.get(0).discount()); money("40000", p.get(0).total()); money("9000", p.get(1).discount());
    }

    @Test void productFixedAmountAppliedOnceAcrossQuantity() {
        var p = CouponDiscountCalculator.plan(coupon("PRODUCT", "AMOUNT", "5000"), List.of(item(1, 1, "3", "30000")), now);
        money("5000", p.getFirst().discount()); money("85000", p.getFirst().total());
    }

    @Test void fixedAmountKeepsExactRemainder() {
        var p = CouponDiscountCalculator.plan(coupon("ORDER", "AMOUNT", "10001"),
                List.of(item(1, 1, "1", "30000"), item(2, 2, "1", "70000")), now);
        money("3000", p.get(0).discount()); money("7001", p.get(1).discount());
    }

    @Test void manySmallLinesCannotProduceNegativeDiscountOrTotal() {
        var items = IntStream.range(1, 9).mapToObj(i -> item(i, i, "1", "1")).toList();
        var p = CouponDiscountCalculator.plan(coupon("ORDER", "PERCENT", "50"), items, now);
        money("4", p.stream().map(CouponDiscountCalculator.Allocation::discount).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertTrue(p.stream().allMatch(a -> a.discount().signum() >= 0 && a.total().signum() >= 0));
    }

    @Test void hundredPercentCanMakeEligibleProductFree() {
        money("0", CouponDiscountCalculator.plan(coupon("PRODUCT", "PERCENT", "100"), List.of(item(1, 1, "2", "100")), now).getFirst().total());
    }

    @Test void legacyCouponDefaultsToWholeOrder() {
        var c = coupon("ORDER", "AMOUNT", "100"); c.remove("scope");
        money("100", CouponDiscountCalculator.plan(c, List.of(item(2, 2, "1", "1000")), now).getFirst().discount());
    }

    @Test void productMinimumUsesWholeBasketBeforeDiscount() {
        var c = coupon("PRODUCT", "PERCENT", "10"); c.put("min_order_amount", new BigDecimal("100000"));
        var p = CouponDiscountCalculator.plan(c, List.of(item(1, 1, "1", "30000"), item(2, 2, "1", "70000")), now);
        money("3000", p.getFirst().discount());
    }

    @Test void insufficientMinimumRejected() {
        var c = coupon("ORDER", "PERCENT", "10"); c.put("min_order_amount", new BigDecimal("1001"));
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(c, List.of(item(1, 1, "1", "1000")), now));
    }

    @Test void missingTargetRejectedEvenWithOtherItems() {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("PRODUCT", "AMOUNT", "10"), List.of(item(2, 2, "1", "1000")), now));
    }

    @Test void productFixedDiscountCannotUseOtherProductValue() {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("PRODUCT", "AMOUNT", "2000"),
                List.of(item(1, 1, "1", "1000"), item(2, 2, "1", "9000")), now));
    }

    @Test void disabledRejected() {
        var c = coupon("ORDER", "AMOUNT", "10"); c.put("lifecycle_status", "DISABLED");
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(c, List.of(item(1, 1, "1", "100")), now));
    }

    @Test void futureStartRejected() {
        var c = coupon("ORDER", "AMOUNT", "10"); c.put("starts_at", Timestamp.from(now.toInstant().plusSeconds(1)));
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(c, List.of(item(1, 1, "1", "100")), now));
    }

    @Test void exactEndTimeRejected() {
        var c = coupon("ORDER", "AMOUNT", "10"); c.put("ends_at", now);
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(c, List.of(item(1, 1, "1", "100")), now));
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "101"})
    void invalidPercentRejected(String value) {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("ORDER", "PERCENT", value), List.of(item(1, 1, "1", "1000")), now));
    }

    @Test void invalidScopeRejected() {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("ITEM", "AMOUNT", "10"), List.of(item(1, 1, "1", "1000")), now));
    }

    @Test void invalidTypeRejected() {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("ORDER", "OTHER", "10"), List.of(item(1, 1, "1", "1000")), now));
    }

    @Test void emptyBasketRejected() {
        assertThrows(BusinessException.class, () -> CouponDiscountCalculator.plan(coupon("ORDER", "AMOUNT", "10"), List.of(), now));
    }
}
