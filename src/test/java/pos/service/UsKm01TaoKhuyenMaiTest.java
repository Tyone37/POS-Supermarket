package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
 * US-KM-01: Tạo chương trình khuyến mãi cho sản phẩm
 * Kiểm thử AC1, AC2, AC3 và các quy tắc nghiệp vụ (múi giờ Asia/Ho_Chi_Minh, [bắt đầu, kết thúc)).
 */
class UsKm01TaoKhuyenMaiTest {
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
        when(db.promotions()).thenReturn(List.of());
    }

    private String offsetInput(int hoursDelta) {
        return LocalDateTime.now(ZONE).plusHours(hoursDelta).format(INPUT_FMT);
    }

    @Test
    @DisplayName("AC1: Tạo khuyến mãi với mức giảm 0 < % <= 100 và thời gian hợp lệ -> lưu thành công")
    void ac1_taoKhuyenMaiHopLe() {
        when(db.overlappingPromotions(eq(10L), any(), any(), eq(null))).thenReturn(List.of());
        when(db.createPromotion(eq("KM20"), eq("Giảm giá sữa tươi"), eq(new BigDecimal("20.00")),
                any(Timestamp.class), any(Timestamp.class), eq(List.of(10L)))).thenReturn(1L);

        Forms.Promotion form = new Forms.Promotion(
                "km20", "Giảm giá sữa tươi", "20",
                offsetInput(1), offsetInput(24), List.of(10L)
        );

        long id = service.create("manager", form);

        assertEquals(1L, id);
        verify(db).createPromotion(eq("KM20"), eq("Giảm giá sữa tươi"), eq(new BigDecimal("20.00")),
                any(Timestamp.class), any(Timestamp.class), eq(List.of(10L)));
    }

    @Test
    @DisplayName("AC1 & Quy tắc: Trạng thái Sắp diễn ra / Đang áp dụng / Đã kết thúc theo nửa khoảng [bắt đầu, kết thúc)")
    void ac1_trangThaiTheoThoiGianNuaMo() {
        Timestamp t0 = Timestamp.from(Instant.now());
        Timestamp before = Timestamp.from(t0.toInstant().minus(1, ChronoUnit.HOURS));
        Timestamp after = Timestamp.from(t0.toInstant().plus(1, ChronoUnit.HOURS));

        Map<String, Object> upcoming = new HashMap<>(Map.of(
                "lifecycle_status", "ENABLED", "starts_at", after, "ends_at", Timestamp.from(after.toInstant().plus(1, ChronoUnit.HOURS))));
        assertEquals("UPCOMING", PromotionService.statusOf(upcoming, t0));

        // Bắt đầu được tính: starts_at == now -> ACTIVE
        Map<String, Object> activeAtStart = new HashMap<>(Map.of(
                "lifecycle_status", "ENABLED", "starts_at", t0, "ends_at", after));
        assertEquals("ACTIVE", PromotionService.statusOf(activeAtStart, t0));

        // Kết thúc không được tính: ends_at == now -> ENDED
        Map<String, Object> endedAtEnd = new HashMap<>(Map.of(
                "lifecycle_status", "ENABLED", "starts_at", before, "ends_at", t0));
        assertEquals("ENDED", PromotionService.statusOf(endedAtEnd, t0));
    }

    @Test
    @DisplayName("AC2: Thiếu thông tin, phần trăm ngoài (0, 100], hoặc kết thúc <= bắt đầu -> báo lỗi và không lưu")
    void ac2_duLieuKhongHopLe() {
        // Thiếu tên
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "   ", "10", offsetInput(1), offsetInput(5), List.of(10L))));

        // Phần trăm <= 0 hoặc > 100
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "KM", "0", offsetInput(1), offsetInput(5), List.of(10L))));
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "KM", "100.01", offsetInput(1), offsetInput(5), List.of(10L))));

        // Không chọn sản phẩm
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "KM", "15", offsetInput(1), offsetInput(5), List.of())));

        // Kết thúc <= bắt đầu
        String same = offsetInput(2);
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "KM", "15", same, same, List.of(10L))));
        assertThrows(BusinessException.class, () -> service.create("manager",
                new Forms.Promotion("KM01", "KM", "15", offsetInput(5), offsetInput(2), List.of(10L))));

        verify(db, never()).createPromotion(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC3: Cùng sản phẩm đã có chương trình khác trùng thời gian áp dụng -> chặn lưu và báo xung đột")
    void ac3_xungDotThoiGianTrenCungSanPham() {
        when(db.overlappingPromotions(eq(10L), any(), any(), eq(null)))
                .thenReturn(List.of(Map.of("id", 99L, "code", "KM_OLD")));

        Forms.Promotion form = new Forms.Promotion(
                "KM_NEW", "Khuyến mãi mới", "15",
                offsetInput(-1), offsetInput(10), List.of(10L)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(ex.getMessage().contains("KM_OLD"));
        verify(db, never()).createPromotion(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Phân quyền: Thu ngân không có quyền tạo chương trình khuyến mãi")
    void phanQuyen_thuNganKhongDuocTao() {
        Forms.Promotion form = new Forms.Promotion(
                "KM01", "KM", "10", offsetInput(1), offsetInput(5), List.of(10L)
        );
        assertThrows(AccessDeniedException.class, () -> service.create("cashier", form));
        verify(db, never()).createPromotion(any(), any(), any(), any(), any(), any());
    }
}
