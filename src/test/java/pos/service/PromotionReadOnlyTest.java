package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;
import pos.exception.BusinessException;
import pos.repository.PosRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionReadOnlyTest {
    private PosRepository db;
    private PromotionService service;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new PromotionService(db);
        when(db.account("manager", false)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", "ACTIVE", "role_code", "MANAGER")));
    }

    @Test
    void danhSachRongKhongYeuCauKhoaGhi() {
        assertTrue(service.list("manager").isEmpty());
        verify(db).account("manager", false);
        verify(db, never()).account(anyString(), eq(true));
    }

    @Test
    void danhSachCoDuLieuDocDuocVaGiuThongTinChuongTrinh() {
        when(db.promotions()).thenReturn(List.of(promotion()));
        when(db.promotionProductIds(10)).thenReturn(List.of(20L));
        var rows = service.list("manager");
        assertEquals(1, rows.size());
        assertEquals("UPCOMING", rows.getFirst().get("effective_status"));
        assertEquals(1, rows.getFirst().get("product_count"));
        assertEquals(new BigDecimal("15.00"), rows.getFirst().get("discount_percent"));
        verify(db, never()).account(anyString(), eq(true));
    }

    @Test
    void chiTietKhongKhoaTaiKhoanHoacChuongTrinh() {
        when(db.promotion(10, false)).thenReturn(Optional.of(promotion()));
        when(db.promotionProductIds(10)).thenReturn(List.of(20L, 21L));
        var detail = service.get("manager", 10);
        assertEquals(List.of(20L, 21L), detail.get("product_ids"));
        assertEquals("UPCOMING", detail.get("effective_status"));
        verify(db).account("manager", false);
        verify(db).promotion(10, false);
        verify(db, never()).account(anyString(), eq(true));
        verify(db, never()).promotion(10, true);
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,ACTIVE", "ADMIN,ACTIVE", "MANAGER,LOCKED"})
    void vanKiemTraQuyenVaTrangThaiTaiKhoan(String role, String state) {
        when(db.account("manager", false)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", state, "role_code", role)));
        assertThrows(AccessDeniedException.class, () -> service.list("manager"));
        assertThrows(AccessDeniedException.class, () -> service.get("manager", 10));
        verify(db, never()).promotions();
        verify(db, never()).promotion(10, false);
    }

    @Test
    void taiKhoanKhongTonTaiKhongDocKhuyenMai() {
        when(db.account("manager", false)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.list("manager"));
        assertThrows(AccessDeniedException.class, () -> service.get("manager", 10));
        verify(db, never()).promotions();
    }

    @Test
    void chiTietKhongTonTaiVanBaoLoiNghiepVu() {
        assertThrows(BusinessException.class, () -> service.get("manager", 999));
        verify(db).promotion(999, false);
    }

    @Test
    void luongGhiVanSuDungKhoaTaiKhoanNhuTruoc() {
        when(db.account("manager", true)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", "ACTIVE", "role_code", "MANAGER")));
        assertThrows(BusinessException.class, () -> service.cancel("manager", 10, false));
        verify(db).account("manager", true);
        verify(db, never()).cancelPromotion(eq(10L), any());
    }

    private Map<String,Object> promotion() {
        return Map.of("id", 10L, "code", "KM-TEST", "name", "Khuyến mãi", "discount_percent", new BigDecimal("15.00"),
                "starts_at", Timestamp.from(Instant.now().plusSeconds(86400)),
                "ends_at", Timestamp.from(Instant.now().plusSeconds(172800)), "lifecycle_status", "ENABLED");
    }
}
