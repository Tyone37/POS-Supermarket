package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponSaleServiceTest {
    private PosRepository db;
    private SaleService sales;
    private Map<String, Object> order;
    private Map<String, Object> coupon;
    private List<Map<String, Object>> items;

    private Map<String, Object> item(long id, long product, String price) {
        return new HashMap<>(Map.of("id", id, "product_id", product, "quantity", BigDecimal.ONE,
                "unit_price", new BigDecimal(price), "discount_amount", BigDecimal.ZERO, "line_total", new BigDecimal(price)));
    }
    @BeforeEach void setup() {
        db = mock(PosRepository.class);
        sales = new SaleService(db, new TransferSettings("970422", "012311234", "DEMO"), new BCryptPasswordEncoder());
        when(db.account("cashier", true)).thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        order = new HashMap<>(Map.of("id", 10L, "shift_id", 20L, "status", "DRAFT", "discount_mode", "NONE"));
        when(db.order(10, true)).thenReturn(Optional.of(order));
        when(db.shift(20)).thenReturn(Optional.of(Map.of("id", 20L, "account_id", 1L, "status", "OPEN")));
        items = new ArrayList<>(List.of(item(101, 1, "30000"), item(102, 2, "70000")));
        when(db.items(10)).thenAnswer(a -> List.copyOf(items));
        when(db.product(anyLong(), eq(true))).thenAnswer(a -> Optional.of(Map.of("id", a.getArgument(0), "status", "ACTIVE",
                "quantity_scale", 0, "stock_quantity", new BigDecimal("100"), "reserved_quantity", BigDecimal.ZERO)));
        coupon = new HashMap<>(Map.of("id", 5L, "scope", "ORDER", "product_id", 1L, "discount_type", "PERCENT",
                "discount_value", BigDecimal.TEN, "min_order_amount", BigDecimal.ZERO, "lifecycle_status", "ENABLED",
                "starts_at", Timestamp.from(Instant.now().minusSeconds(3600)), "ends_at", Timestamp.from(Instant.now().plusSeconds(3600))));
        when(db.couponByCode("SALE10")).thenReturn(Optional.of(coupon));
        doAnswer(a -> {
            var item = items.stream().filter(i -> ((Long) i.get("id")).equals(a.getArgument(0))).findFirst().orElseThrow();
            item.put("quantity", a.getArgument(1)); item.put("discount_amount", a.getArgument(2)); item.put("line_total", a.getArgument(3));
            item.put("promotion_id", a.getArgument(4)); return null;
        }).when(db).updateItemPromotion(anyLong(), any(), any(), any(), any(), any(), any());
        doAnswer(a -> { order.put("discount_mode", a.getArgument(1)); order.put("coupon_code_snapshot", a.getArgument(3)); return null; })
                .when(db).orderDiscount(anyLong(), any(), any(), any(), any());
        doAnswer(a -> { order.put("subtotal_amount", a.getArgument(1)); order.put("discount_amount", a.getArgument(2)); order.put("total_amount", a.getArgument(3)); return null; })
                .when(db).totals(anyLong(), any(), any(), any());
        doAnswer(a -> { items.removeIf(i -> ((Long) i.get("id")).equals(a.getArgument(0))); return null; }).when(db).removeItem(anyLong());
    }
    private void money(String value, Object actual) { assertEquals(0, new BigDecimal(value).compareTo((BigDecimal) actual)); }
    private void apply() { sales.applyCoupon("cashier", 10, " sale10 ", true); }
    private void noInventoryWrites() {
        verify(db, never()).deductStock(anyLong(), any(), any());
        verify(db, never()).createPayment(anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(db, never()).markPaid(anyLong(), any());
    }

    @Test void wholeOrderCouponCanonicalizesCodeAndTotals() {
        apply(); money("10000", order.get("discount_amount")); money("90000", order.get("total_amount"));
        assertEquals("COUPON", order.get("discount_mode")); assertEquals("SALE10", order.get("coupon_code_snapshot"));
    }
    @Test void productCouponOnlyDiscountsTargetLine() {
        coupon.put("scope", "PRODUCT"); apply();
        money("3000", items.getFirst().get("discount_amount")); money("0", items.getLast().get("discount_amount")); money("97000", order.get("total_amount"));
    }
    @Test void autoPromotionCannotBeReplacedWithoutConfirmation() {
        items.getFirst().put("promotion_id", 9L);
        assertThrows(BusinessException.class, () -> sales.applyCoupon("cashier", 10, "SALE10", false));
        verify(db, never()).updateItemPromotion(anyLong(), any(), any(), any(), any(), any(), any());
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any());
    }
    @Test void confirmedReplacementClearsAutomaticPromotionMetadata() {
        items.getFirst().put("promotion_id", 9L); apply();
        assertNull(items.getFirst().get("promotion_id"));
        verify(db).updateItemPromotion(eq(101L), any(), any(), any(), isNull(), isNull(), isNull());
    }
    @Test void productCouponRecalculatedWhenQuantityChanges() {
        coupon.put("scope", "PRODUCT"); apply(); sales.quantity("cashier", 10, 101, "3", false);
        money("9000", items.getFirst().get("discount_amount")); money("151000", order.get("total_amount"));
    }
    @Test void removingUnrelatedItemRetainsProductCoupon() {
        coupon.put("scope", "PRODUCT"); apply(); sales.quantity("cashier", 10, 102, "0", true);
        assertEquals("COUPON", order.get("discount_mode")); money("27000", order.get("total_amount"));
    }
    @Test void removingTargetAutomaticallyClearsCoupon() {
        coupon.put("scope", "PRODUCT"); apply(); sales.quantity("cashier", 10, 101, "0", true);
        assertEquals("NONE", order.get("discount_mode")); money("70000", order.get("total_amount"));
    }
    @Test void fallingBelowMinimumClearsCoupon() {
        coupon.put("min_order_amount", new BigDecimal("90000")); apply(); sales.quantity("cashier", 10, 102, "0", true);
        assertEquals("NONE", order.get("discount_mode")); money("30000", order.get("total_amount"));
    }
    @Test void manualDiscountMustBeReconfirmedAfterCartEdit() {
        sales.applyDirectDiscount("cashier", 10, new Forms.DirectDiscount("ORDER", null, "PERCENT", "5", null, "", ""));
        sales.quantity("cashier", 10, 101, "2", false);
        assertEquals("NONE", order.get("discount_mode")); money("130000", order.get("total_amount"));
    }
    @Test void expiredCouponBlocksPaymentWithoutDeductingStock() {
        apply(); coupon.put("ends_at", Timestamp.from(Instant.now().minusSeconds(1)));
        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10, new Forms.Payment("CASH", "100000", null, "coupon-pay-key", null)));
        noInventoryWrites(); money("90000", order.get("total_amount"));
    }
    @Test void changedCouponRequiresReapplyBeforePayment() {
        apply(); coupon.put("discount_value", new BigDecimal("20"));
        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10, new Forms.Payment("CASH", "100000", null, "coupon-pay-key", null)));
        noInventoryWrites(); money("90000", order.get("total_amount"));
    }
    @Test void paidInvoiceCannotReceiveNewCoupon() {
        order.put("status", "PAID"); assertThrows(BusinessException.class, this::apply);
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any()); noInventoryWrites();
    }
}
