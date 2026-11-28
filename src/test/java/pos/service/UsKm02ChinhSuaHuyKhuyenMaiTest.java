package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

/**
 * US-KM-02: Chỉnh sửa / Hủy khuyến mãi
 * Kiểm thử AC1, AC2, AC3, AC4 và các quy tắc nghiệp vụ.
 */
class UsKm02ChinhSuaHuyKhuyenMaiTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private PosRepository db;
    private PromotionService service;

    @BeforeEach
    void setUp() {
        db = mock(PosRepository.class);
        service = new PromotionService(db);

        when(db.account("manager", true))
                .thenReturn(Optional.of(Map.of("id", 2L, "status", "ACTIVE", "role_code", "MANAGER")));
        when(db.account("cashier", true))
                .thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.product(10L, false))
                .thenReturn(Optional.of(Map.of("id", 10L, "code", "SP001", "name", "Sữa tươi")));
    }

    private String futureInput(int hoursAhead) {
        return LocalDateTime.now(ZONE).plusHours(hoursAhead).format(INPUT_FMT);
    }

    private Map<String, Object> promo(long id, String code, Timestamp startsAt, Timestamp endsAt, String lifecycleStatus) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("code", code);
        map.put("name", "Khuyến mãi " + code);
        map.put("discount_percent", new BigDecimal("15.00"));
        map.put("starts_at", startsAt);
        map.put("ends_at", endsAt);
        map.put("lifecycle_status", lifecycleStatus);
        map.put("canceled_at", "CANCELED".equals(lifecycleStatus) ? Timestamp.from(Instant.now()) : null);
        return map;
    }

    @Test
    @DisplayName("AC1: Chương trình chưa bắt đầu (UPCOMING) sửa phần trăm và thời gian hợp lệ -> cập nhật thành công")
    void ac1_suaChuongTrinhChuaBatDauHopLe() {
        Timestamp start = Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS));
        Timestamp end = Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS));
        when(db.promotion(100L, true)).thenReturn(Optional.of(promo(100L, "KM_UP", start, end, "ENABLED")));
        when(db.overlappingPromotions(eq(10L), any(), any(), eq(100L))).thenReturn(List.of());

        Forms.Promotion form = new Forms.Promotion(
                "KM_UP", "Giảm giá cuối tuần mới", "25",
                futureInput(24), futureInput(72), List.of(10L)
        );

        service.update("manager", 100L, form);

        verify(db).updatePromotion(eq(100L), eq("Giảm giá cuối tuần mới"), eq(new BigDecimal("25.00")),
                any(Timestamp.class), any(Timestamp.class), eq(List.of(10L)));
    }

    @Test
    @DisplayName("AC2: Chương trình đang áp dụng (ACTIVE) khi hủy/kết thúc sớm với xác nhận -> đặt CANCELED")
    void ac2_ketThucSomChuongTrinhDangApDung() {
        Timestamp start = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Timestamp end = Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS));
        when(db.promotion(200L, true)).thenReturn(Optional.of(promo(200L, "KM_ACT", start, end, "ENABLED")));

        service.cancel("manager", 200L, true);

        verify(db).cancelPromotion(eq(200L), any(Timestamp.class));
    }

    @Test
    @DisplayName("AC2: Hủy chương trình khi chưa xác nhận -> báo lỗi và không thay đổi dữ liệu")
    void ac2_huyChuaXacNhan() {
        assertThrows(BusinessException.class, () -> service.cancel("manager", 200L, false));
        verify(db, never()).cancelPromotion(anyLong(), any());
    }

    @Test
    @DisplayName("Quy tắc nghiệp vụ: Chương trình đang áp dụng (ACTIVE) không được sửa hồi tố")
    void quyTac_khongSuaChuongTrinhDangApDung() {
        Timestamp start = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
        Timestamp end = Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS));
        when(db.promotion(200L, true)).thenReturn(Optional.of(promo(200L, "KM_ACT", start, end, "ENABLED")));

        Forms.Promotion form = new Forms.Promotion(
                "KM_ACT", "Sửa tên", "30",
                futureInput(1), futureInput(48), List.of(10L)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("manager", 200L, form));
        assertTrue(ex.getMessage().contains("đang áp dụng"));
        verify(db, never()).updatePromotion(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC3: Chương trình đã kết thúc (ENDED) hoặc đã hủy (CANCELED) chỉ được xem, không thể sửa hoặc hủy lại")
    void ac3_chuongTrinhDaKetThucChiDuocXem() {
        Timestamp start = Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS));
        Timestamp end = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        when(db.promotion(300L, true)).thenReturn(Optional.of(promo(300L, "KM_END", start, end, "ENABLED")));

        Forms.Promotion form = new Forms.Promotion(
                "KM_END", "Sửa lại", "10",
                futureInput(1), futureInput(24), List.of(10L)
        );

        assertThrows(BusinessException.class, () -> service.update("manager", 300L, form));
        assertThrows(BusinessException.class, () -> service.cancel("manager", 300L, true));
        verify(db, never()).updatePromotion(anyLong(), any(), any(), any(), any(), any());
        verify(db, never()).cancelPromotion(anyLong(), any());
    }

    @Test
    @DisplayName("AC4: Sửa phần trăm không hợp lệ (> 100 hoặc <= 0) hoặc thời gian kết thúc <= bắt đầu -> báo lỗi và giữ nguyên dữ liệu cũ")
    void ac4_duLieuSuaKhongHopLe() {
        Timestamp start = Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS));
        Timestamp end = Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS));
        when(db.promotion(100L, true)).thenReturn(Optional.of(promo(100L, "KM_UP", start, end, "ENABLED")));

        Forms.Promotion invalidPercent = new Forms.Promotion(
                "KM_UP", "Tên hợp lệ", "120",
                futureInput(24), futureInput(48), List.of(10L)
        );
        assertThrows(BusinessException.class, () -> service.update("manager", 100L, invalidPercent));

        Forms.Promotion invalidTime = new Forms.Promotion(
                "KM_UP", "Tên hợp lệ", "20",
                futureInput(48), futureInput(24), List.of(10L)
        );
        assertThrows(BusinessException.class, () -> service.update("manager", 100L, invalidTime));

        verify(db, never()).updatePromotion(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC4: Sửa thời gian gây trùng lặp trên cùng sản phẩm -> báo lỗi và giữ nguyên dữ liệu cũ")
    void ac4_trungThoiGianTrenCungSanPham() {
        Timestamp start = Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS));
        Timestamp end = Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS));
        when(db.promotion(100L, true)).thenReturn(Optional.of(promo(100L, "KM_UP", start, end, "ENABLED")));

        Map<String, Object> conflict = promo(999L, "KM_OTHER", start, end, "ENABLED");
        when(db.overlappingPromotions(eq(10L), any(), any(), eq(100L))).thenReturn(List.of(conflict));

        Forms.Promotion form = new Forms.Promotion(
                "KM_UP", "Tên mới", "20",
                futureInput(24), futureInput(72), List.of(10L)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("manager", 100L, form));
        assertTrue(ex.getMessage().contains("KM_OTHER"));
        verify(db, never()).updatePromotion(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Phân quyền: Thu ngân (CASHIER) không có quyền sửa hoặc hủy khuyến mãi")
    void phanQuyen_thuNganKhongDuocSuaHuy() {
        Forms.Promotion form = new Forms.Promotion(
                "KM_UP", "Tên mới", "20",
                futureInput(24), futureInput(72), List.of(10L)
        );
        assertThrows(AccessDeniedException.class, () -> service.update("cashier", 100L, form));
        assertThrows(AccessDeniedException.class, () -> service.cancel("cashier", 100L, true));
    }
}
