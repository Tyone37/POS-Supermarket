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
import pos.dto.ProductEditForm;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum29SuaSanPhamTest {
    private PosRepository db;
    private CatalogService service;
    private Map<String, Object> product;
    private ProductEditForm form;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new CatalogService(db);
        var actor = Optional.of(Map.<String, Object>of("id", 1L, "status", "ACTIVE", "role_code", "MANAGER"));
        when(db.account("manager", true)).thenReturn(actor);
        when(db.account("manager", false)).thenReturn(actor);
        product = new HashMap<>(Map.of("id", 20L, "code", "SP20", "barcode", "001234", "name", "Tên cũ",
                "unit_id", 1L, "category_id", 2L, "stock_quantity", new BigDecimal("10"),
                "reserved_quantity", new BigDecimal("2"), "updated_at", time(), "status", "ACTIVE"));
        product.put("sale_price", new BigDecimal("20000"));
        product.put("quantity_scale", 0);
        when(db.product(20, true)).thenReturn(Optional.of(product));
        when(db.product(20, false)).thenReturn(Optional.of(product));
        when(db.units()).thenReturn(List.of(Map.of("id", 1L, "code", "CAI", "quantity_scale", 0),
                Map.of("id", 3L, "code", "KG", "quantity_scale", 3)));
        when(db.categories()).thenReturn(List.of(Map.of("id", 2L, "code", "UNCLASSIFIED")));
        when(db.products(null)).thenReturn(List.of(product));
        form = valid();
    }

    private Timestamp time() {
        return Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"));
    }

    private ProductEditForm valid() {
        var form = new ProductEditForm();
        form.setBarcode("001234");
        form.setName("Tên mới");
        form.setCategoryId(2L);
        form.setUnitId(1L);
        form.setPrice("25000");
        form.setStock("15");
        form.setImageUrl("");
        form.setExpectedUpdatedAt(time().toInstant().toString());
        return form;
    }

    private void noWrite() {
        verify(db, never()).updateProduct(anyLong(), anyString(), anyString(), anyLong(), anyLong(),
                any(), any(), any(), any());
    }

    @Test
    void luuHopLeVaLoaiTruMaVachCuaChinhSanPham() {
        service.update("manager", 20, form);
        verify(db).product(20, true);
        verify(db).updateProduct(eq(20L), eq("001234"), eq("Tên mới"), eq(2L), eq(1L),
                eq(new BigDecimal("25000.00")), eq(new BigDecimal("15")), isNull(), any());
        assertEquals("SP20", product.get("code"));
    }

    @Test
    void boKhoangTrangNhungGiuSoKhongDauMaVach() {
        form.setBarcode("  00000042 ");
        form.setName("  Tên mới  ");
        service.update("manager", 20, form);
        verify(db).updateProduct(eq(20L), eq("00000042"), eq("Tên mới"), eq(2L), eq(1L), any(), any(), isNull(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"barcode", "code"})
    void chanMaVachTrungCaBarcodeVaSkuKhac(String field) {
        var other = new HashMap<>(Map.<String, Object>of("id", 21L, "code", "SP21", "barcode", "OTHER"));
        other.put(field, "001234");
        when(db.products(null)).thenReturn(List.of(product, other));
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void sanPhamNgungBanVanThamGiaKiemTraTrung() {
        var other = Map.<String, Object>of("id", 21L, "code", "SP21", "barcode", "001234", "status", "STOPPED");
        when(db.products(null)).thenReturn(List.of(product, other));
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "-1", "abc", "12.5", "10000000000000000"})
    void giaSaiKhongGhiDuLieu(String value) {
        form.setPrice(value);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
        assertEquals(new BigDecimal("20000"), product.get("sale_price"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "abc", "1.2", "1000000000000"})
    void tonKhoSaiKhongGhiDuLieu(String value) {
        form.setStock(value);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void tonKhoKhongDuocNhoHonSoDangGiu() {
        form.setStock("1");
        var error = assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        assertTrue(error.getMessage().contains("đang giữ"));
        noWrite();
    }

    @Test
    void tonKhoBangLuongDangGiuDuocChapNhan() {
        form.setStock("2");
        service.update("manager", 20, form);
        verify(db).updateProduct(eq(20L), anyString(), anyString(), eq(2L), eq(1L), any(), eq(new BigDecimal("2")), isNull(), any());
    }

    @Test
    void donViLeChoPhepBaChuSoThapPhan() {
        product.put("unit_id", 3L);
        form.setUnitId(3L);
        form.setStock("12.345");
        service.update("manager", 20, form);
        verify(db).updateProduct(eq(20L), anyString(), anyString(), eq(2L), eq(3L), any(), eq(new BigDecimal("12.345")), isNull(), any());
    }

    @Test
    void donViLeKhongChoQuaBaChuSoThapPhan() {
        form.setUnitId(3L);
        form.setStock("12.3456");
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void thieuTenHoacMaVachBiTuChoi(String value) {
        form.setName(value);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        form.setName("Tên mới");
        form.setBarcode(value);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void tenVaMaVachVuotDoDaiBiTuChoi() {
        form.setName("N".repeat(201));
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        form.setName("Tên mới");
        form.setBarcode("0".repeat(51));
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void nhomVaDonViBatBuocHopLe() {
        form.setCategoryId(null);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        form.setCategoryId(999L);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        form.setCategoryId(2L);
        form.setUnitId(null);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        form.setUnitId(999L);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.com/a.png", "javascript:alert(1)", "abc"})
    void anhKhongHopLeKhongLuu(String value) {
        form.setImageUrl(value);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void luuAnhHttpsHopLe() {
        form.setImageUrl(" https://example.com/a.png ");
        service.update("manager", 20, form);
        verify(db).updateProduct(eq(20L), anyString(), anyString(), eq(2L), eq(1L), any(), any(), eq("https://example.com/a.png"), any());
    }

    @Test
    void donViCoLichSuKhongDuocDoi() {
        form.setUnitId(3L);
        when(db.hasProductHistory(20)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @Test
    void donViChuaCoLichSuDuocDoi() {
        form.setUnitId(3L);
        service.update("manager", 20, form);
        verify(db).updateProduct(eq(20L), anyString(), anyString(), eq(2L), eq(3L), any(), any(), isNull(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "2026-11-24T00:00:00Z"})
    void formCuHoacSaiPhienBanKhongGhiDe(String revision) {
        form.setExpectedUpdatedAt(revision);
        assertThrows(BusinessException.class, () -> service.update("manager", 20, form));
        noWrite();
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,ACTIVE", "ADMIN,ACTIVE", "MANAGER,LOCKED"})
    void khongCoQuyenQuanLySanPhamBiTuChoi(String role, String status) {
        var account = Optional.of(Map.<String, Object>of("id", 1L, "role_code", role, "status", status));
        when(db.account("manager", true)).thenReturn(account);
        when(db.account("manager", false)).thenReturn(account);
        assertThrows(AccessDeniedException.class, () -> service.update("manager", 20, form));
        assertThrows(AccessDeniedException.class, () -> service.editable("manager", 20));
        verify(db, never()).product(anyLong(), anyBoolean());
        noWrite();
    }

    @Test
    void moFormTraDuLieuVaCoKhoaDonViLichSu() {
        when(db.hasProductHistory(20)).thenReturn(true);
        var result = service.editable("manager", 20);
        assertEquals("001234", result.get("barcode"));
        assertEquals(true, result.get("unit_locked"));
        verify(db).product(20, false);
        noWrite();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 999})
    void maSanPhamKhongTonTaiHoacKhongHopLe(long id) {
        assertThrows(BusinessException.class, () -> service.update("manager", id, form));
        noWrite();
    }

    @Test
    void giaoDichMoiLayGiaMoiNhungHoaDonCuGiuGiaSnapshot() {
        var oldLine = Map.<String, Object>of("product_name_snapshot", "Tên cũ", "unit_price", new BigDecimal("20000"));
        when(db.invoice(81)).thenReturn(Optional.of(new HashMap<>(Map.of("id", 81L, "order_id", 9L, "issued_by", 7L))));
        when(db.items(9)).thenReturn(List.of(oldLine));
        var sale = new SaleService(db, new TransferSettings("970422", "123", "DEMO"));
        service.update("manager", 20, form);
        var current = new HashMap<>(product);
        current.put("sale_price", new BigDecimal("25000.00"));
        current.put("name", "Tên mới");
        when(db.product(20, true)).thenReturn(Optional.of(current));
        when(db.account("cashier", true)).thenReturn(Optional.of(Map.of("id", 7L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.order(10, true)).thenReturn(Optional.of(Map.of("id", 10L, "shift_id", 30L, "status", "DRAFT")));
        when(db.shift(30)).thenReturn(Optional.of(Map.of("id", 30L, "account_id", 7L, "status", "OPEN")));
        when(db.productsByCode("001234")).thenReturn(List.of(current));
        sale.add("cashier", 10, "001234");
        verify(db).createItem(10, current);
        @SuppressWarnings("unchecked")
        var history = (List<Map<String, Object>>) sale.invoice("manager", 81).get("items");
        assertEquals(new BigDecimal("20000"), history.getFirst().get("unit_price"));
        assertEquals("Tên cũ", history.getFirst().get("product_name_snapshot"));
    }
}
