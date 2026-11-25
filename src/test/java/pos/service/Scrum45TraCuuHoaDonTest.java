package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import pos.dto.InvoiceSearch;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.InvoiceSearchRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Scrum45TraCuuHoaDonTest {
    private AccountRepository accounts;
    private InvoiceSearchRepository db;
    private InvoiceSearchService service;
    private InvoiceSearch.Filter filter;

    @BeforeEach
    void setup() {
        accounts = mock(AccountRepository.class);
        db = mock(InvoiceSearchRepository.class);
        service = new InvoiceSearchService(accounts, db);
        filter = new InvoiceSearch.Filter();
        account("CASHIER", "ACTIVE");
    }

    private void account(String role, String status) {
        when(accounts.findByUsername("user")).thenReturn(Optional.of(
                new Account(7L, "user", "unused", "Thu ngân", role, status, Set.of("INVOICE_READ"))));
    }

    @Test
    void thuNganChiTraCuuHoaDonDoMinhLap() {
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query("", "", "", 7L, 0), 21);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void quanLyVaAdminTraCuuToanHeThong(String role) {
        account(role, "ACTIVE");
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query("", "", "", null, 0), 21);
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,LOCKED", "MANAGER,LOCKED", "ADMIN,LOCKED", "UNKNOWN,ACTIVE"})
    void khongDuQuyenKhongDocHoaDon(String role, String status) {
        account(role, status);
        assertThrows(AccessDeniedException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @Test
    void taiKhoanKhongConTonTaiBiTuChoi() {
        when(accounts.findByUsername("user")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @CsvSource({"'', '', ''", "HD-01, '', ''", "'', CASH, ''", "'', '', ISSUED",
            "HD, BANK_TRANSFER, ''", "HD, '', VOID", "'', CASH, ISSUED", "HD, BANK_TRANSFER, VOID"})
    void ketHopTatCaCacNhanhBoLoc(String code, String method, String status) {
        filter.setCode(code);
        filter.setMethod(method);
        filter.setStatus(status);
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query(code, method, status, 7L, 0), 21);
    }

    @Test
    void loaiKhoangTrangHaiDauMaTimKiem() {
        filter.setCode("  HD-123  ");
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query("HD-123", "", "", 7L, 0), 21);
    }

    @Test
    void boLocNullTuongDuongTatCaTrongPhamVi() {
        filter.setCode(null);
        filter.setMethod(null);
        filter.setStatus(null);
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query("", "", "", 7L, 0), 21);
    }

    @Test
    void khongCoKetQuaKhongBaoLoi() {
        var result = service.search("user", filter, 0);
        assertTrue(result.items().isEmpty());
        assertEquals(false, result.hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CARD", "cash", "CASH' OR 1=1", " "})
    void phuongThucKhongHopLeBiTuChoi(String method) {
        filter.setMethod(method);
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PAID", "CANCELLED", "issued", " "})
    void trangThaiPhaiLaTrangThaiHoaDon(String status) {
        filter.setStatus(status);
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @Test
    void maVuotDoDaiBiTuChoi() {
        filter.setCode("H".repeat(31));
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @Test
    void maDoDaiToiDaDuocTim() {
        filter.setCode("H".repeat(30));
        service.search("user", filter, 0);
        verify(db).search(new InvoiceSearch.Query("H".repeat(30), "", "", 7L, 0), 21);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void trangSaiKhongTruyVan(int page) {
        assertThrows(BusinessException.class, () -> service.search("user", filter, page));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 20, 21})
    void phanTrangGiuThuTuVaSoTienTuDatabase(int count) {
        var rows = IntStream.range(0, count).mapToObj(i -> new InvoiceSearch.Row(i, "HD-" + i,
                Timestamp.from(Instant.parse("2026-11-25T00:00:00Z")), "Thu ngân", "CASH", "ISSUED",
                new BigDecimal("12345.67"))).toList();
        when(db.search(any(), eq(21))).thenReturn(rows);
        var result = service.search("user", filter, 2);
        assertEquals(rows.subList(0, Math.min(count, 20)), result.items());
        assertEquals(2, result.page());
        assertEquals(count > 20, result.hasNext());
        verify(db).search(new InvoiceSearch.Query("", "", "", 7L, 40), 21);
        assertThrows(UnsupportedOperationException.class, () -> result.items().clear());
    }
}
