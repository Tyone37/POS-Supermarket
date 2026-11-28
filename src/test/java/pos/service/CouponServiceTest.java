package pos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pos.dto.CouponForm;
import pos.exception.BusinessException;
import pos.repository.PosRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponServiceTest {
    private PosRepository db;
    private CouponService service;

    @BeforeEach void setup() {
        db = mock(PosRepository.class); service = new CouponService(db);
        var manager = Optional.of(Map.<String, Object>of("id", 1L, "status", "ACTIVE", "role_code", "MANAGER"));
        when(db.account("manager", true)).thenReturn(manager);
        when(db.account("manager", false)).thenReturn(manager);
        when(db.product(7, true)).thenReturn(Optional.of(Map.of("id", 7L, "status", "ACTIVE")));
    }

    private CouponForm form(String scope, Long product, String type, String value, String minimum, String end) {
        return new CouponForm(" sale10 ", "Giảm giá", scope, product, type, value, minimum,
                "2026-11-28T09:00", end, "ENABLED");
    }
    private CouponForm valid() { return form("ORDER", null, "PERCENT", "10", "0", "2026-11-29T09:00"); }

    @Test void savesCanonicalWholeOrderCouponAndUtcTime() {
        service.save("manager", null, valid());
        verify(db).saveCoupon(isNull(), eq("SALE10"), eq("Giảm giá"), eq("PERCENT"), eq(new BigDecimal("10.00")),
                eq(new BigDecimal("0.00")), eq(java.sql.Timestamp.from(java.time.Instant.parse("2026-11-28T02:00:00Z"))),
                any(), eq("ENABLED"), eq("ORDER"), isNull());
    }
    @Test void savesProductCoupon() {
        service.save("manager", null, form("PRODUCT", 7L, "AMOUNT", "1000", "0", "2026-11-29T09:00"));
        verify(db).saveCoupon(isNull(), anyString(), anyString(), eq("AMOUNT"), eq(new BigDecimal("1000.00")), any(), any(), any(), anyString(), eq("PRODUCT"), eq(7L));
    }
    @Test void wholeOrderIgnoresStaleProductSelection() {
        service.save("manager", null, form("ORDER", 7L, "PERCENT", "10", "0", "2026-11-29T09:00"));
        verify(db, never()).product(anyLong(), anyBoolean());
        verify(db).saveCoupon(isNull(), anyString(), anyString(), anyString(), any(), any(), any(), any(), anyString(), eq("ORDER"), isNull());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "101", "1.001", "abc", ""})
    void rejectsInvalidPercent(String value) {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("ORDER", null, "PERCENT", value, "0", "2026-11-29T09:00")));
        verify(db, never()).saveCoupon(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.5", "abc"})
    void rejectsInvalidFixedAmount(String value) {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("ORDER", null, "AMOUNT", value, "0", "2026-11-29T09:00")));
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "0.5", "", "abc"})
    void rejectsInvalidMinimum(String minimum) {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("ORDER", null, "PERCENT", "10", minimum, "2026-11-29T09:00")));
    }
    @ParameterizedTest @ValueSource(strings = {"2026-11-28T09:00", "2026-11-27T09:00", "bad", ""})
    void rejectsInvalidEndTime(String end) {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("ORDER", null, "PERCENT", "10", "0", end)));
    }
    @Test void duplicateCodeRejected() {
        when(db.couponByCode("SALE10")).thenReturn(Optional.of(Map.of("id", 9L)));
        assertThrows(BusinessException.class, () -> service.save("manager", null, valid()));
    }
    @Test void codeCannotChangeAfterCreation() {
        when(db.coupon(9, true)).thenReturn(Optional.of(Map.of("code", "OLD")));
        assertThrows(BusinessException.class, () -> service.save("manager", 9L, valid()));
    }
    @Test void existingCouponCanBeUpdatedWithoutDuplicateSelfCheck() {
        when(db.coupon(9, true)).thenReturn(Optional.of(Map.of("code", "SALE10")));
        service.save("manager", 9L, valid()); verify(db, never()).couponByCode(any());
        verify(db).saveCoupon(eq(9L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    @Test void missingProductRejected() {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("PRODUCT", null, "PERCENT", "10", "0", "2026-11-29T09:00")));
    }
    @Test void stoppedProductRejected() {
        when(db.product(7, true)).thenReturn(Optional.of(Map.of("status", "STOPPED")));
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("PRODUCT", 7L, "PERCENT", "10", "0", "2026-11-29T09:00")));
    }
    @Test void nonexistentProductRejected() {
        assertThrows(BusinessException.class, () -> service.save("manager", null, form("PRODUCT", 99L, "PERCENT", "10", "0", "2026-11-29T09:00")));
    }
    @Test void stoppedProductCouponCanStillBeDisabled() {
        when(db.product(7, true)).thenReturn(Optional.of(Map.of("status", "STOPPED")));
        var f = form("PRODUCT", 7L, "PERCENT", "10", "0", "2026-11-29T09:00");
        service.save("manager", null, new CouponForm(f.code(), f.name(), f.scope(), f.productId(), f.type(),
                f.value(), f.minimum(), f.startsAt(), f.endsAt(), "DISABLED"));
        verify(db).saveCoupon(isNull(), any(), any(), any(), any(), any(), any(), any(), eq("DISABLED"), eq("PRODUCT"), eq(7L));
    }
    @ParameterizedTest @ValueSource(strings = {"CASHIER", "ADMIN"})
    void nonManagerCannotSaveOrList(String role) {
        var actor = Optional.of(Map.<String, Object>of("id", 2L, "status", "ACTIVE", "role_code", role));
        when(db.account("other", true)).thenReturn(actor); when(db.account("other", false)).thenReturn(actor);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.save("other", null, valid()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.list("other", "", 0));
        verify(db, never()).coupons(any(), anyInt(), anyInt());
    }
    @Test void emptyListAndReadOnlyActorDoNotLock() {
        assertTrue(service.list("manager", " missing ", 0).items().isEmpty());
        verify(db).coupons("missing", 0, 21); verify(db, never()).account("manager", true);
    }
    @Test void paginationReturnsTwentyAndNextPageFlag() {
        when(db.coupons("", 20, 21)).thenReturn(IntStream.range(0, 21).mapToObj(i -> Map.<String, Object>of("id", (long) i)).toList());
        var result = service.list("manager", "", 1); assertEquals(20, result.items().size()); assertTrue(result.hasNext());
    }
    @Test void invalidPageRejected() { assertThrows(BusinessException.class, () -> service.list("manager", "", -1)); }
    @Test void missingCouponRejected() { assertThrows(BusinessException.class, () -> service.get("manager", 99)); }
}
