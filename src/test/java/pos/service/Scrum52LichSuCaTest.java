package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import pos.dto.ShiftHistory;
import pos.dto.ShiftInvoice;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.ShiftHistoryRepository;
import pos.repository.PosRepository;
import pos.config.TransferSettings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class Scrum52LichSuCaTest {
    private AccountRepository accounts;
    private ShiftHistoryRepository db;
    private ShiftHistoryService service;
    private ShiftHistory.Filter filter;

    @BeforeEach
    void setup() {
        accounts = mock(AccountRepository.class);
        db = mock(ShiftHistoryRepository.class);
        service = new ShiftHistoryService(accounts, db);
        filter = new ShiftHistory.Filter();
        account("MANAGER", "ACTIVE");
    }

    private void account(String role, String status) {
        when(accounts.findByUsername("user")).thenReturn(Optional.of(
                new Account(1L, "user", "unused", "Người dùng", role, status, Set.of())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void haiVaiTroDuocXemCa(String role) {
        account(role, "ACTIVE");
        assertTrue(service.search("user", filter, 0).shifts().items().isEmpty());
        verify(db).search(any(), eq(21));
    }

    @ParameterizedTest
    @CsvSource({"CASHIER,ACTIVE", "MANAGER,LOCKED", "ADMIN,LOCKED", "OTHER,ACTIVE"})
    void tuChoiTruocMoiTruyVanLichSu(String role, String status) {
        account(role, status);
        assertThrows(AccessDeniedException.class, () -> service.search("user", filter, 0));
        assertThrows(AccessDeniedException.class, () -> service.detail("user", 1, 0));
        assertThrows(AccessDeniedException.class, () -> service.options("user"));
        verifyNoInteractions(db);
    }

    @Test
    void taiKhoanKhongTonTaiBiTuChoi() {
        when(accounts.findByUsername("user")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @Test
    void ketHopBonBoLocVaDoiNgayVietNamSangUtc() {
        filter.setFromDate(LocalDate.parse("2026-11-25"));
        filter.setToDate(LocalDate.parse("2026-11-26"));
        filter.setCashierId(17L);
        filter.setRegisterId(9L);
        filter.setStatus("CLOSED");
        service.search("user", filter, 2);
        var captor = ArgumentCaptor.forClass(ShiftHistory.Query.class);
        verify(db).search(captor.capture(), eq(21));
        var query = captor.getValue();
        assertEquals(Instant.parse("2026-11-24T17:00:00Z"), query.fromInclusive().toInstant());
        assertEquals(Instant.parse("2026-11-26T17:00:00Z"), query.toExclusive().toInstant());
        assertEquals(17L, query.cashierId());
        assertEquals(9L, query.registerId());
        assertEquals("CLOSED", query.status());
        assertEquals(40, query.offset());
    }

    @ParameterizedTest
    @CsvSource({"2026-11-25,2026-11-25T17:00:00Z", "2024-02-29,2024-02-29T17:00:00Z", "2026-12-31,2026-12-31T17:00:00Z"})
    void ngayKetThucBaoGomTronNgay(String date, String end) {
        filter.setFromDate(LocalDate.parse(date));
        filter.setToDate(LocalDate.parse(date));
        service.search("user", filter, 0);
        verify(db).search(argThat(q -> q.toExclusive().toInstant().equals(Instant.parse(end))
                && q.toExclusive().getTime() - q.fromInclusive().getTime() == 86_400_000), eq(21));
    }

    @Test
    void boLocRongKhongThemGioiHan() {
        service.search("user", filter, 0);
        verify(db).search(new ShiftHistory.Query(null, null, null, null, "", 0), 21);
    }

    @Test
    void chiNgayBatDau() {
        filter.setFromDate(LocalDate.parse("2026-11-25"));
        service.search("user", filter, 0);
        verify(db).search(argThat(q -> q.fromInclusive() != null && q.toExclusive() == null), eq(21));
    }

    @Test
    void chiNgayKetThuc() {
        filter.setToDate(LocalDate.parse("2026-11-25"));
        service.search("user", filter, 0);
        verify(db).search(argThat(q -> q.fromInclusive() == null && q.toExclusive() != null), eq(21));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "OPEN", "CLOSED"})
    void chapNhanTrangThaiHopLe(String status) {
        filter.setStatus(status);
        service.search("user", filter, 0);
        verify(db).search(argThat(q -> q.status().equals(status)), eq(21));
    }

    @Test
    void trangThaiNullDuocChuanHoa() {
        filter.setStatus(null);
        service.search("user", filter, 0);
        verify(db).search(argThat(q -> q.status().isEmpty()), eq(21));
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed", "INVALID", "OPEN' OR 1=1", " "})
    void trangThaiSaiKhongTruyVan(String status) {
        filter.setStatus(status);
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @Test
    void khoangNgayNguocBiTuChoi() {
        filter.setFromDate(LocalDate.parse("2026-11-26"));
        filter.setToDate(LocalDate.parse("2026-11-25"));
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 999, 9999})
    void namNgoaiMienDbBiTuChoi(int year) {
        filter.setToDate(LocalDate.of(year, 1, 1));
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void idThuNganVaQuayPhaiDuong(long id) {
        filter.setCashierId(id);
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        filter.setCashierId(null);
        filter.setRegisterId(id);
        assertThrows(BusinessException.class, () -> service.search("user", filter, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void trangAmHoacTranOffsetBiTuChoi(int page) {
        assertThrows(BusinessException.class, () -> service.search("user", filter, page));
        assertThrows(BusinessException.class, () -> service.detail("user", 1, page));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 20, 21})
    void phanTrangCaKhongLamMatHoacHienThiBanGhiThamDo(int count) {
        var rows = IntStream.range(0, count).mapToObj(i -> row(i + 1, "CLOSED")).toList();
        when(db.search(any(), eq(21))).thenReturn(rows);
        var page = service.search("user", filter, 1).shifts();
        assertEquals(rows.subList(0, Math.min(count, 20)), page.items());
        assertEquals(count > 20, page.hasNext());
        assertEquals(1, page.number());
    }

    @Test
    void giuLuaChonLichSuTuRepository() {
        var cashiers = List.of(new ShiftHistory.Choice(8, "Nhân viên đã nghỉ"));
        var registers = List.of(new ShiftHistory.Choice(4, "Quầy đã dừng"));
        when(db.cashiers()).thenReturn(cashiers);
        when(db.registers()).thenReturn(registers);
        assertEquals(new ShiftHistory.Options(cashiers, registers), service.search("user", filter, 0).options());
    }

    @Test
    void giuNguyenSoLieuDaChotKhongTinhLaiTuHoaDon() {
        var detail = detail();
        when(db.detail(30)).thenReturn(Optional.of(detail));
        when(db.invoices(30, 0, 21)).thenReturn(List.of(invoice(1, "999999")));
        var result = service.detail("user", 30, 0);
        assertSame(detail, result.detail());
        assertEquals(new BigDecimal("6000"), result.detail().closingRevenue());
        assertEquals(new BigDecimal("-1000"), result.detail().closingDifference());
        verify(db).detail(30);
        verify(db).invoices(30, 0, 21);
        verifyNoMoreInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 20, 21})
    void phanTrangHoaDonDungCa(int count) {
        when(db.detail(30)).thenReturn(Optional.of(detail()));
        var invoices = IntStream.range(0, count).mapToObj(i -> invoice(i, "6000")).toList();
        when(db.invoices(30, 40, 21)).thenReturn(invoices);
        var page = service.detail("user", 30, 2).invoices();
        assertEquals(Math.min(count, 20), page.items().size());
        assertEquals(count > 20, page.hasNext());
        assertEquals(2, page.number());
        verify(db).invoices(30, 40, 21);
    }

    @Test
    void caMoKhongGiaLapSoLieuDaChot() {
        var detail = new ShiftHistory.Detail(row(30, "OPEN"), BigDecimal.TEN, null, null, null, null, null, null);
        when(db.detail(30)).thenReturn(Optional.of(detail));
        assertNull(service.detail("user", 30, 0).detail().closingActualCash());
    }

    @Test
    void caKhongTonTaiKhongTruyVanHoaDon() {
        assertThrows(BusinessException.class, () -> service.detail("user", 999, 0));
        verify(db, never()).invoices(anyLong(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void maCaKhongHopLe(long id) {
        assertThrows(BusinessException.class, () -> service.detail("user", id, 0));
        verifyNoInteractions(db);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void quanLyVaAdminDuocMoHoaDonThuNganKhac(String role) {
        var sales = mock(PosRepository.class);
        when(sales.account("user", true)).thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", role)));
        when(sales.invoice(7)).thenReturn(Optional.of(new HashMap<>(Map.of("id", 7L, "order_id", 8L, "issued_by", 99L))));
        var service = new SaleService(sales, new TransferSettings("970422", "123", "DEMO"));
        assertEquals(7L, service.invoice("user", 7).get("id"));
    }

    private ShiftHistory.Row row(long id, String status) {
        var time = Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"));
        return new ShiftHistory.Row(id, "CA-" + id, status, 10, "Thu ngân", "NV10", 1, "Quầy 1", time,
                status.equals("CLOSED") ? time : null);
    }

    private ShiftHistory.Detail detail() {
        return new ShiftHistory.Detail(row(30, "CLOSED"), new BigDecimal("50000"), new BigDecimal("6000"),
                new BigDecimal("6000"), BigDecimal.ZERO, new BigDecimal("56000"), new BigDecimal("55000"), new BigDecimal("-1000"));
    }

    private ShiftInvoice invoice(long id, String amount) {
        return new ShiftInvoice(id, "HD-" + id, "Thu ngân", Timestamp.from(Instant.now()), new BigDecimal(amount), "CASH");
    }
}
