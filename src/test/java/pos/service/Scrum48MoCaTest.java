package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SCRUM-48 — test riêng theo user story. */
class Scrum48MoCaTest {
    private PosRepository db;
    private ShiftService service;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new ShiftService(db);
        when(db.account("cashier", true))
                .thenReturn(
                        Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.employee(1L)).thenReturn(Optional.of(Map.of("id", 10L)));
        when(db.registers()).thenReturn(List.of(Map.of("id", 20L)));
    }

    // Mở ca với tiền đầu ca hợp lệ.
    @Test
    void moCaVoiTienHopLe() {
        when(db.createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(30L);
        long id = service.open("cashier", 20L, "500000", "open-key-123");
        assertEquals(30L, id);
    }

    // Từ chối tiền đầu ca âm.
    @Test
    void moCaVoiTienAm() {
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 20L, "-1", "open-key-123"));
    }

    // Yêu cầu chọn quầy khi mở ca.
    @Test
    void moCaChuaChonQuay() {
        assertThrows(
                BusinessException.class,
                () -> service.open("cashier", null, "500000", "open-key-123"));
    }

    // Từ chối tiền đầu ca chứa chữ.
    @Test
    void moCaNhapChuVaoTienDauCa() {
        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () -> service.open("cashier", 20L, "abc", "open-key-123"));
        assertTrue(error.getMessage().contains("Tiền đầu ca"));
    }

    // Từ chối tiền đầu ca trống.
    @Test
    void moCaBoTrongTienDauCa() {
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 20L, "", "open-key-123"));
    }

    // Từ chối tiền đầu ca có phần lẻ.
    @Test
    void moCaNhapTienLe() {
        assertThrows(
                BusinessException.class,
                () -> service.open("cashier", 20L, "500.5", "open-key-123"));
    }

    // Cho phép mở ca với tiền đầu ca bằng không.
    @Test
    void moCaVoiKhongDong() {
        when(db.createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(30L);
        assertEquals(30L, service.open("cashier", 20L, "0", "open-key-123"));
        verify(db)
                .createShift(
                        anyString(),
                        eq(10L),
                        eq(20L),
                        eq(new BigDecimal("0.00")),
                        eq("open-key-123"),
                        any());
    }

    // Từ chối quầy không tồn tại.
    @Test
    void moCaQuayKhongTonTai() {
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 999L, "0", "open-key-123"));
        verify(db, never())
                .createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any());
    }

    // Từ chối quầy đã ngừng hoạt động.
    @Test
    void moCaQuayDaNgungHoatDong() {
        // Repository chỉ trả quầy đang hoạt động, quầy 20 đã bị loại khỏi danh sách.
        when(db.registers()).thenReturn(List.of());
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 20L, "0", "open-key-123"));
    }

    // Từ chối mở thêm khi đang có ca mở.
    @Test
    void moCaKhiDangCoCaMo() {
        when(db.latestShift(10L)).thenReturn(Optional.of(Map.of("id", 30L, "status", "OPEN")));
        assertEquals(30L, service.open("cashier", 20L, "0", "open-key-123"));
        verify(db, never())
                .createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any());
    }

    // Gửi lại yêu cầu không tạo thêm ca.
    @Test
    void guiLaiYeuCauMoCaKhongTaoThemCa() {
        when(db.shiftByKey("open-key-123"))
                .thenReturn(
                        Optional.of(
                                Map.of(
                                        "id",
                                        30L,
                                        "employee_id",
                                        10L,
                                        "register_id",
                                        20L,
                                        "opening_cash",
                                        new BigDecimal("500000.00"))));
        assertEquals(30L, service.open("cashier", 20L, "500000", "open-key-123"));
        assertEquals(30L, service.open("cashier", 20L, "500000", "open-key-123"));
        verify(db, never())
                .createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any());
    }

    // Từ chối mở ca khi thiếu hồ sơ nhân viên.
    @Test
    void moCaThieuHoSoNhanVien() {
        when(db.employee(1L)).thenReturn(Optional.empty());
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 20L, "0", "open-key-123"));
    }

    // Chặn quản lý mở ca.
    @Test
    void quanLyKhongDuocMoCa() {
        when(db.account("manager", true))
                .thenReturn(
                        Optional.of(Map.of("id", 2L, "status", "ACTIVE", "role_code", "MANAGER")));
        assertThrows(
                AccessDeniedException.class,
                () -> service.open("manager", 20L, "0", "open-key-123"));
        verify(db, never())
                .createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any());
    }
}
