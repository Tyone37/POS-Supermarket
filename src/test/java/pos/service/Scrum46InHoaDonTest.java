package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.PosRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class Scrum46InHoaDonTest {
    private AccountRepository accounts;
    private PosRepository db;
    private InvoicePrintService service;
    private Map<String, Object> invoice;
    private Map<String, Object> item;

    @BeforeEach
    void setup() {
        accounts = mock(AccountRepository.class);
        db = mock(PosRepository.class);
        service = new InvoicePrintService(accounts, db);
        account("CASHIER", "ACTIVE");
        invoice = new HashMap<>(Map.of("id", 81L, "order_id", 10L, "issued_by", 7L,
                "invoice_no", "HD-81", "issued_at", Timestamp.from(Instant.parse("2026-11-25T01:00:00Z")),
                "cashier_name", "Tên lúc bán", "method", "CASH", "status", "ISSUED"));
        invoice.put("subtotal_amount", new BigDecimal("12345.67"));
        invoice.put("discount_amount", new BigDecimal("345.67"));
        invoice.put("total_amount", new BigDecimal("12000.00"));
        invoice.put("amount_received", new BigDecimal("20000.00"));
        invoice.put("change_amount", new BigDecimal("8000.00"));
        item = Map.of("product_code_snapshot", "SP-CU", "product_name_snapshot", "Tên hàng lúc bán",
                "unit_code_snapshot", "KG", "quantity", new BigDecimal("1.250"),
                "unit_price", new BigDecimal("9876.54"), "discount_amount", new BigDecimal("345.67"),
                "line_total", new BigDecimal("12000.00"));
        when(db.invoice(81)).thenReturn(Optional.of(invoice));
        when(db.invoiceItems(10)).thenReturn(List.of(item));
    }

    private void account(String role, String status) {
        when(accounts.findByUsername("user")).thenReturn(Optional.of(
                new Account(7L, "user", "unused", "Tên đã đổi", role, status, Set.of())));
    }

    @Test
    void banInDocDungSnapshotKhongTinhLaiTien() {
        var preview = service.preview("user", 81);
        assertEquals(81, preview.id());
        assertEquals("HD-81", preview.invoiceNo());
        assertEquals(invoice.get("issued_at"), preview.issuedAt());
        assertEquals("Tên lúc bán", preview.cashierName());
        assertEquals(new BigDecimal("12345.67"), preview.subtotal());
        assertEquals(new BigDecimal("345.67"), preview.discount());
        assertEquals(new BigDecimal("12000.00"), preview.total());
        assertEquals(new BigDecimal("20000.00"), preview.received());
        assertEquals(new BigDecimal("8000.00"), preview.change());
        var line = preview.items().getFirst();
        assertEquals("Tên hàng lúc bán", line.productName());
        assertEquals("SP-CU", line.productCode());
        assertEquals("KG", line.unitCode());
        assertEquals(new BigDecimal("1.250"), line.quantity());
        assertEquals(new BigDecimal("9876.54"), line.unitPrice());
        assertEquals(new BigDecimal("345.67"), line.discount());
        assertEquals(new BigDecimal("12000.00"), line.total());
        verify(db).invoice(81);
        verify(db).invoiceItems(10);
        verifyNoMoreInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void quanLyVaAdminDuocInHoaDonThuNganKhac(String role) {
        account(role, "ACTIVE");
        invoice.put("issued_by", 999L);
        assertEquals("HD-81", service.preview("user", 81).invoiceNo());
    }

    @Test
    void thuNganKhongInDuocHoaDonKhacNguoiLap() {
        invoice.put("issued_by", 999L);
        assertThrows(AccessDeniedException.class, () -> service.preview("user", 81));
        verify(db, never()).invoiceItems(anyLong());
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,LOCKED", "MANAGER,LOCKED", "ADMIN,LOCKED", "OTHER,ACTIVE"})
    void khongDuQuyenKhongDocHoaDon(String role, String status) {
        account(role, status);
        assertThrows(AccessDeniedException.class, () -> service.preview("user", 81));
        verifyNoInteractions(db);
    }

    @Test
    void taiKhoanKhongTonTai() {
        when(accounts.findByUsername("user")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.preview("user", 81));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void idKhongHopLeKhongTruyVan(long id) {
        assertThrows(BusinessException.class, () -> service.preview("user", id));
        verifyNoInteractions(db);
    }

    @Test
    void hoaDonChuaDuocLuuKhongTaoBanMoi() {
        assertThrows(BusinessException.class, () -> service.preview("user", 999));
        verify(db).invoice(999);
        verifyNoMoreInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
    void phuongThucVaMaGiaoDichTuDuLieuDaLuu(String method) {
        invoice.put("method", method);
        invoice.put("bank_reference", "FT-LICH-SU");
        var preview = service.preview("user", 81);
        assertEquals(method, preview.method());
        assertEquals("FT-LICH-SU", preview.bankReference());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISSUED", "VOID"})
    void inLaiKhongDoiTrangThaiHoacTongTien(String status) {
        invoice.put("status", status);
        var before = new HashMap<>(invoice);
        var first = service.preview("user", 81);
        var second = service.preview("user", 81);
        assertEquals(first, second);
        assertEquals(before, invoice);
        assertEquals(status, first.status());
        verify(db, times(2)).invoice(81);
        verify(db, times(2)).invoiceItems(10);
        verifyNoMoreInteractions(db);
    }

    @Test
    void danhSachDongHoaDonKhongBiSuaSauKhiDoc() {
        var preview = service.preview("user", 81);
        assertThrows(UnsupportedOperationException.class, () -> preview.items().clear());
        assertEquals(1, preview.items().size());
    }

    @Test
    void caDaDongKhongCanMoLaiDeIn() {
        var preview = service.preview("user", 81);
        assertTrue(preview.total().signum() > 0);
        verify(db).invoice(81);
        verify(db).invoiceItems(10);
        verifyNoMoreInteractions(db);
    }
}
