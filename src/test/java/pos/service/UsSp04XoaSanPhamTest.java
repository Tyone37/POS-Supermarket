package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import pos.exception.BusinessException;
import pos.repository.PosRepository;

/**
 * US-SP-04: Xóa sản phẩm đã ngừng bán
 * Kiểm thử AC1, AC2, AC3, AC4 và quy tắc nghiệp vụ.
 */
class UsSp04XoaSanPhamTest {
    private PosRepository db;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        db = mock(PosRepository.class);
        service = new CatalogService(db);

        when(db.account("manager", true))
                .thenReturn(Optional.of(Map.of("id", 2L, "status", "ACTIVE", "role_code", "MANAGER")));
        when(db.account("cashier", true))
                .thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
    }

    @Test
    @DisplayName("AC1: Danh sách màn hình xóa chỉ lọc sản phẩm Ngừng bán; sản phẩm Đang bán bị chặn khi xóa")
    void ac1_chiHienThiVaChoPhepXoaSanPhamNgungBan() {
        Map<String, Object> active = Map.of(
                "id", 1L, "code", "SP01", "barcode", "89301", "name", "Sữa tươi",
                "status", "ACTIVE", "sale_price", new BigDecimal("30000"));
        Map<String, Object> stopped = Map.of(
                "id", 2L, "code", "SP02", "barcode", "89302", "name", "Bánh cũ",
                "status", "STOPPED", "sale_price", new BigDecimal("15000"));
        when(db.products(null)).thenReturn(List.of(active, stopped));

        var result = service.stoppedProducts("");
        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().get("id"));

        when(db.product(1L, true)).thenReturn(Optional.of(active));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("manager", 1L, true));
        assertTrue(ex.getMessage().contains("ngừng bán"));
        verify(db, never()).deleteProduct(anyLong());
    }

    @Test
    @DisplayName("AC2: Xác nhận xóa sản phẩm Ngừng bán chưa có trên hóa đơn -> xóa vĩnh viễn thành công")
    void ac2_xoaVinhVienSanPhamNgungBanChuaCoHoaDon() {
        Map<String, Object> stopped = Map.of(
                "id", 2L, "code", "SP02", "name", "Bánh cũ", "status", "STOPPED");
        when(db.product(2L, true)).thenReturn(Optional.of(stopped));
        when(db.productHasInvoice(2L)).thenReturn(false);
        when(db.productHasOrderItem(2L)).thenReturn(false);

        service.delete("manager", 2L, true);

        verify(db).deleteProduct(2L);
    }

    @Test
    @DisplayName("AC3: Sản phẩm đã xuất hiện trên ít nhất một hóa đơn -> chặn xóa, thông báo lý do và giữ nguyên dữ liệu")
    void ac3_chanXoaNeuDaCoTrenHoaDon() {
        Map<String, Object> stopped = Map.of(
                "id", 2L, "code", "SP02", "name", "Bánh cũ", "status", "STOPPED");
        when(db.product(2L, true)).thenReturn(Optional.of(stopped));
        when(db.productHasInvoice(2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("manager", 2L, true));
        assertTrue(ex.getMessage().contains("hóa đơn"));
        verify(db, never()).deleteProduct(anyLong());
    }

    @Test
    @DisplayName("AC4: Chưa xác nhận xóa (confirmed = false) -> không làm thay đổi dữ liệu")
    void ac4_khongXoaKhiChuaXacNhan() {
        assertThrows(BusinessException.class, () -> service.delete("manager", 2L, false));
        verify(db, never()).deleteProduct(anyLong());
    }

    @Test
    @DisplayName("Quy tắc nghiệp vụ: Thu ngân không có quyền xóa sản phẩm")
    void phanQuyen_thuNganKhongDuocXoa() {
        assertThrows(AccessDeniedException.class, () -> service.delete("cashier", 2L, true));
        verify(db, never()).deleteProduct(anyLong());
    }
}
