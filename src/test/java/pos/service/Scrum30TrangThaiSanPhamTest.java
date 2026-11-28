package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import pos.config.TransferSettings;
import pos.dto.ProductStatusForm;
import pos.exception.BusinessException;
import pos.repository.PosRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum30TrangThaiSanPhamTest {
    private PosRepository db;
    private CatalogService catalog;
    private SaleService sales;
    private Map<String, Object> product;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        catalog = new CatalogService(db);
        sales = new SaleService(db, new TransferSettings("970422", "123", "DEMO"));
        when(db.account("manager", true)).thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "MANAGER")));
        when(db.account("cashier", true)).thenReturn(Optional.of(Map.of("id", 7L, "status", "ACTIVE", "role_code", "CASHIER")));
        product = new HashMap<>(Map.of("id", 20L, "code", "SP20", "barcode", "001234", "name", "Sữa tươi",
                "status", "ACTIVE", "sale_price", new BigDecimal("20000"), "stock_quantity", new BigDecimal("10"),
                "reserved_quantity", BigDecimal.ZERO, "quantity_scale", 0,
                "updated_at", Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"))));
        when(db.product(20, true)).thenReturn(Optional.of(product));
        when(db.products(null)).thenReturn(List.of(product));
        when(db.order(10, true)).thenReturn(Optional.of(Map.of("id", 10L, "shift_id", 30L, "status", "DRAFT")));
        when(db.shift(30)).thenReturn(Optional.of(Map.of("id", 30L, "account_id", 7L, "status", "OPEN")));
        doAnswer(call -> {
            product.put("status", call.getArgument(1));
            product.put("updated_at", call.getArgument(2));
            product.put("stopped_at", "STOPPED".equals(call.getArgument(1)) ? call.getArgument(2) : null);
            return null;
        }).when(db).changeProductStatus(eq(20L), anyString(), any());
    }

    private ProductStatusForm form(String status) {
        var form = new ProductStatusForm();
        form.setStatus(ProductStatusForm.Status.valueOf(status));
        form.setExpectedUpdatedAt(((Timestamp) product.get("updated_at")).toInstant().toString());
        return form;
    }

    private void noWrite() {
        verify(db, never()).changeProductStatus(anyLong(), anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ACTIVE"})
    void doiTrangThaiDuocKhoaVaLuuVoiGioHeThong(String target) {
        product.put("status", target.equals("STOPPED") ? "ACTIVE" : "STOPPED");
        catalog.changeStatus("manager", 20, form(target));
        verify(db).product(20, true);
        verify(db).changeProductStatus(eq(20L), eq(target), any(Timestamp.class));
        assertEquals(target, product.get("status"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ACTIVE"})
    void guiLapGiuNguyenThoiDiemDaLuu(String target) {
        product.put("status", target.equals("STOPPED") ? "ACTIVE" : "STOPPED");
        var form = form(target);
        catalog.changeStatus("manager", 20, form);
        var first = new HashMap<>(product);
        catalog.changeStatus("manager", 20, form);
        assertEquals(first, product);
        verify(db, times(1)).changeProductStatus(eq(20L), eq(target), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "2026-11-24T00:00:00Z"})
    void danhSachCuHoacPhienBanSaiKhongDoiTrangThai(String revision) {
        var form = form("STOPPED");
        form.setExpectedUpdatedAt(revision);
        assertThrows(BusinessException.class, () -> catalog.changeStatus("manager", 20, form));
        assertEquals("ACTIVE", product.get("status"));
        noWrite();
    }

    @Test
    void yeuCauCuKhongNgungBanLaiSauKhiDaKichHoat() {
        var stale = form("STOPPED");
        catalog.changeStatus("manager", 20, stale);
        catalog.changeStatus("manager", 20, form("ACTIVE"));
        // Ensure a distinct saved revision even if the mock's two calls happen in one millisecond.
        product.put("updated_at", Timestamp.from(Instant.parse("2026-11-26T01:00:00Z")));
        assertThrows(BusinessException.class, () -> catalog.changeStatus("manager", 20, stale));
        assertEquals("ACTIVE", product.get("status"));
        verify(db, times(2)).changeProductStatus(eq(20L), anyString(), any());
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,ACTIVE", "ADMIN,ACTIVE", "MANAGER,LOCKED", "OTHER,ACTIVE"})
    void chiQuanLyDangHoatDongDuocDoi(String role, String status) {
        when(db.account("manager", true)).thenReturn(Optional.of(Map.of("id", 1L, "role_code", role, "status", status)));
        assertThrows(AccessDeniedException.class, () -> catalog.changeStatus("manager", 20, form("STOPPED")));
        verify(db, never()).product(anyLong(), anyBoolean());
        noWrite();
    }

    @Test
    void taiKhoanKhongTonTaiBiTuChoi() {
        when(db.account("manager", true)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> catalog.changeStatus("manager", 20, form("STOPPED")));
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 999})
    void sanPhamKhongHopLeHoacKhongTonTai(long id) {
        assertThrows(BusinessException.class, () -> catalog.changeStatus("manager", id, form("STOPPED")));
        noWrite();
    }

    @Test
    void trangThaiNullBiTuChoi() {
        var form = form("STOPPED");
        form.setStatus(null);
        assertThrows(BusinessException.class, () -> catalog.changeStatus("manager", 20, form));
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sua", "SỮA", "SP20"})
    void ngungBanKhongXuatHienTrongTimKiemTaiQuay(String query) {
        catalog.changeStatus("manager", 20, form("STOPPED"));
        assertTrue(catalog.search(query, null, true).isEmpty());
    }

    @Test
    void ngungBanVanHienTrongDanhMucQuanLy() {
        catalog.changeStatus("manager", 20, form("STOPPED"));
        var rows = catalog.search("001234", null, false);
        assertEquals(1, rows.size());
        assertEquals("STOPPED", rows.getFirst().get("status"));
        assertEquals(new BigDecimal("10"), rows.getFirst().get("stock_quantity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"001234", "SP20"})
    void quetMaNgungBanBiTuChoiKeCaKetQuaTraVeTuTrangCu(String code) {
        var stale = new HashMap<>(product);
        catalog.changeStatus("manager", 20, form("STOPPED"));
        when(db.productsByCode(code)).thenReturn(List.of(stale));
        assertThrows(BusinessException.class, () -> sales.add("cashier", 10, code));
        verify(db, never()).createItem(anyLong(), any(), any(), any(), any(), any(), any(), any());
        verify(db, never()).items(10);
    }

    @Test
    void quetKhongTimThaySanPhamDangBanKhongTaoDong() {
        assertThrows(BusinessException.class, () -> sales.add("cashier", 10, "001234"));
        verify(db, never()).createItem(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sua", "SP20"})
    void kichHoatLaiXuatHienTrongTimKiem(String query) {
        product.put("status", "STOPPED");
        catalog.changeStatus("manager", 20, form("ACTIVE"));
        assertEquals(1, catalog.search(query, null, true).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"001234", "SP20"})
    void kichHoatLaiQuetDuocVaThemDongMoi(String code) {
        product.put("status", "STOPPED");
        catalog.changeStatus("manager", 20, form("ACTIVE"));
        when(db.productsByCode(code)).thenReturn(List.of(product));
        sales.add("cashier", 10, code);
        verify(db).createItem(eq(10L), eq(product), eq(BigDecimal.ONE), eq(BigDecimal.ZERO),
                eq(new BigDecimal("20000.00")), isNull(), isNull(), isNull());
    }

    @Test
    void hoaDonCuVaGiaSnapshotKhongDoiKhiNgungVaKichHoat() {
        var saved = new HashMap<String,Object>(Map.of("id", 81L, "order_id", 9L, "issued_by", 7L, "status", "ISSUED"));
        var line = Map.<String,Object>of("product_name_snapshot", "Tên lúc bán", "unit_price", new BigDecimal("18000"));
        when(db.invoice(81)).thenReturn(Optional.of(saved));
        when(db.items(9)).thenReturn(List.of(line));
        catalog.changeStatus("manager", 20, form("STOPPED"));
        assertEquals(List.of(line), sales.invoice("manager", 81).get("items"));
        catalog.changeStatus("manager", 20, form("ACTIVE"));
        assertEquals(List.of(line), sales.invoice("manager", 81).get("items"));
        assertEquals("ISSUED", saved.get("status"));
        assertEquals(new BigDecimal("18000"), line.get("unit_price"));
        assertEquals(new BigDecimal("20000"), product.get("sale_price"));
    }
}
