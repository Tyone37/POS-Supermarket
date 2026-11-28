package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;

import pos.dto.ShiftInvoice;
import pos.repository.PosRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum49TomTatCaTest {
    private PosRepository db;
    private ShiftService service;
    private Map<String, Object> shift;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new ShiftService(db);
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.employee(1L)).thenReturn(Optional.of(Map.of("id", 10L)));
        shift = Map.of("id", 30L, "status", "OPEN", "opening_cash", new BigDecimal("500000.00"),
                "opened_at", Timestamp.from(Instant.parse("2026-11-24T01:00:00Z")));
        when(db.latestShift(10L)).thenReturn(Optional.of(shift));
    }

    @Test
    void caMoChuaCoHoaDonHienThiKhong() {
        var summary = service.summary("cashier");

        assertEquals(0, summary.invoiceCount());
        assertEquals(0, summary.revenue().signum());
        assertEquals(0, summary.cashRevenue().signum());
        assertTrue(summary.invoices().isEmpty());
        assertEquals(shift, summary.shift());
        verify(db).completedShiftInvoices(30L, 1L);
    }

    @Test
    void tongHopTienSauGiamVaGiuThongTinMoCa() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "77600.00", "CASH"), invoice(101L, "24000.00", "BANK_TRANSFER")));

        var summary = service.summary("cashier");

        assertEquals(2, summary.invoiceCount());
        assertEquals(new BigDecimal("101600.00"), summary.revenue());
        assertEquals(new BigDecimal("77600.00"), summary.cashRevenue());
        assertEquals(shift.get("opened_at"), summary.shift().get("opened_at"));
        assertEquals(shift.get("opening_cash"), summary.shift().get("opening_cash"));
    }

    @Test
    void taiLaiTrangLaySoLieuMoiTuMayChu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of())
                .thenReturn(List.of(invoice(100L, "20000.00", "CASH")));

        assertEquals(0, service.summary("cashier").invoiceCount());
        assertEquals(new BigDecimal("20000.00"), service.summary("cashier").revenue());
    }

    @Test
    void chuaMoCaKhongLayHoaDonCu() {
        when(db.latestShift(10L)).thenReturn(Optional.empty());

        var summary = service.summary("cashier");

        assertNull(summary.shift());
        assertEquals(0, summary.invoiceCount());
        assertEquals(0, summary.revenue().signum());
        verify(db, never()).completedShiftInvoices(anyLong(), anyLong());
    }

    @Test
    void caDaDongKhongTinhVaoTomTatCaDangMo() {
        when(db.latestShift(10L)).thenReturn(Optional.of(Map.of("id", 29L, "status", "CLOSED")));

        var summary = service.summary("cashier");

        assertEquals(0, summary.invoiceCount());
        assertEquals(0, summary.revenue().signum());
        verify(db, never()).completedShiftInvoices(anyLong(), anyLong());
    }

    @Test
    void khongLayCaCuaThuNganKhac() {
        when(db.account("other", true)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.employee(2L)).thenReturn(Optional.of(Map.of("id", 11L)));
        when(db.latestShift(11L)).thenReturn(Optional.of(Map.of("id", 31L, "status", "OPEN")));

        service.summary("other");

        verify(db).completedShiftInvoices(31L, 2L);
        verify(db, never()).completedShiftInvoices(30L, 2L);
    }

    @Test
    void tuChoiTaiKhoanKhoa() {
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "status", "LOCKED", "role_code", "CASHIER")));

        assertThrows(AccessDeniedException.class, () -> service.summary("cashier"));
        verify(db, never()).latestShift(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void tuChoiVaiTroKhac(String role) {
        when(db.account("other", true)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", "ACTIVE", "role_code", role)));

        assertThrows(AccessDeniedException.class, () -> service.summary("other"));
        verify(db, never()).latestShift(anyLong());
    }

    @Test
    void tuChoiTaiKhoanKhongTonTaiTruocKhiDocDuLieuCa() {
        assertThrows(AccessDeniedException.class, () -> service.summary("missing"));

        verify(db).account("missing", true);
        verifyNoMoreInteractions(db);
    }

    @Test
    void tuChoiThuNganKhongCoHoSoNhanVien() {
        when(db.employee(1L)).thenReturn(Optional.empty());

        assertThrows(pos.exception.BusinessException.class, () -> service.summary("cashier"));
        verify(db, never()).latestShift(anyLong());
    }

    @ParameterizedTest
    @CsvSource({
            "CASH, 0.00, 0.00",
            "CASH, 20000.00, 20000.00",
            "BANK_TRANSFER, 24000.00, 0.00",
            "CASH, 9999999999999999.00, 9999999999999999.00",
            "BANK_TRANSFER, 9999999999999999.00, 0.00"
    })
    void tinhMotHoaDonTheoPhuongThucVaBienTien(String method, String total, String cash) {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, total, method)));

        var summary = service.summary("cashier");

        assertEquals(1, summary.invoiceCount());
        assertEquals(0, new BigDecimal(total).compareTo(summary.revenue()));
        assertEquals(0, new BigDecimal(cash).compareTo(summary.cashRevenue()));
    }

    @Test
    void nhieuHoaDonTienMatCongDayDu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "10000.00", "CASH"), invoice(101L, "20000.00", "CASH"),
                invoice(102L, "30000.00", "CASH")));

        var summary = service.summary("cashier");

        assertEquals(3, summary.invoiceCount());
        assertEquals(new BigDecimal("60000.00"), summary.revenue());
        assertEquals(summary.revenue(), summary.cashRevenue());
    }

    @Test
    void nhieuHoaDonChuyenKhoanKhongTangDoanhThuTienMat() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "10000.00", "BANK_TRANSFER"), invoice(101L, "20000.00", "BANK_TRANSFER")));

        var summary = service.summary("cashier");

        assertEquals(2, summary.invoiceCount());
        assertEquals(new BigDecimal("30000.00"), summary.revenue());
        assertEquals(0, summary.cashRevenue().signum());
    }

    @Test
    void hoaDonKhacNhauCungSoTienVanDemDayDu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "20000.00", "CASH"), invoice(101L, "20000.00", "CASH")));

        var summary = service.summary("cashier");

        assertEquals(2, summary.invoiceCount());
        assertEquals(new BigDecimal("40000.00"), summary.revenue());
    }

    @Test
    void tongNhieuHoaDonKhongMatDoChinhXacKhiVuotMienSoJavascript() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "9999999999999999.00", "CASH"), invoice(101L, "2.00", "CASH")));

        var summary = service.summary("cashier");

        assertEquals(new BigDecimal("10000000000000001.00"), summary.revenue());
        assertEquals(summary.revenue(), summary.cashRevenue());
    }

    @Test
    void duLieuDecimalDaLuuKhongBiLamTronLai() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "0.10", "CASH"), invoice(101L, "0.20", "BANK_TRANSFER")));

        var summary = service.summary("cashier");

        assertEquals(new BigDecimal("0.30"), summary.revenue());
        assertEquals(new BigDecimal("0.10"), summary.cashRevenue());
    }

    @Test
    void khongPhuThuocThuTuHoaDon() {
        var cash = invoice(100L, "77600.00", "CASH");
        var bank = invoice(101L, "24000.00", "BANK_TRANSFER");
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(cash, bank)).thenReturn(List.of(bank, cash));

        var first = service.summary("cashier");
        var second = service.summary("cashier");

        assertEquals(first.invoiceCount(), second.invoiceCount());
        assertEquals(first.revenue(), second.revenue());
        assertEquals(first.cashRevenue(), second.cashRevenue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "500000.00", "9999999999999999.00"})
    void tienDauCaKhongCongVaoDoanhThu(String openingCash) {
        var current = new HashMap<>(shift);
        current.put("opening_cash", new BigDecimal(openingCash));
        when(db.latestShift(10L)).thenReturn(Optional.of(current));
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));

        var summary = service.summary("cashier");

        assertEquals(new BigDecimal(openingCash), summary.shift().get("opening_cash"));
        assertEquals(new BigDecimal("20000.00"), summary.revenue());
        assertEquals(new BigDecimal("20000.00"), summary.cashRevenue());
    }

    @Test
    void khongDocLaiGiaSanPhamHoacDonDeTinhDoanhThu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "77600.00", "CASH")));

        var summary = service.summary("cashier");

        assertEquals(new BigDecimal("77600.00"), summary.revenue());
        verify(db).account("cashier", true);
        verify(db).employee(1L);
        verify(db).latestShift(10L);
        verify(db).completedShiftInvoices(30L, 1L);
        verifyNoMoreInteractions(db);
    }

    @Test
    void taiLaiNhieuLanKhongCongDonDoanhThuCu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));

        var first = service.summary("cashier");
        var second = service.summary("cashier");

        assertEquals(1, first.invoiceCount());
        assertEquals(1, second.invoiceCount());
        assertEquals(new BigDecimal("20000.00"), second.revenue());
        verify(db, times(2)).completedShiftInvoices(30L, 1L);
    }

    @Test
    void taiLaiKhiKhongConHoaDonHopLeTraVeKhong() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")))
                .thenReturn(List.of());

        assertEquals(1, service.summary("cashier").invoiceCount());
        var refreshed = service.summary("cashier");

        assertEquals(0, refreshed.invoiceCount());
        assertEquals(0, refreshed.revenue().signum());
        assertEquals(0, refreshed.cashRevenue().signum());
    }

    @Test
    void dongCaSauLanXemTruocKhongGiuSoLieuCu() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));
        assertEquals(1, service.summary("cashier").invoiceCount());
        when(db.latestShift(10L)).thenReturn(Optional.of(Map.of("id", 30L, "status", "CLOSED")));

        var refreshed = service.summary("cashier");

        assertEquals(0, refreshed.invoiceCount());
        assertEquals(0, refreshed.revenue().signum());
        verify(db, times(1)).completedShiftInvoices(30L, 1L);
    }

    @Test
    void moCaMoiSauCaCuChiDocHoaDonCaMoi() {
        when(db.latestShift(10L)).thenReturn(Optional.of(Map.of("id", 29L, "status", "CLOSED")))
                .thenReturn(Optional.of(shift));
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));

        assertEquals(0, service.summary("cashier").invoiceCount());
        assertEquals(new BigDecimal("20000.00"), service.summary("cashier").revenue());
        verify(db, never()).completedShiftInvoices(29L, 1L);
    }

    @Test
    void doiCaDangMoKhongCongHoaDonCaTruoc() {
        when(db.latestShift(10L)).thenReturn(Optional.of(shift))
                .thenReturn(Optional.of(Map.of("id", 32L, "status", "OPEN")));
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));
        when(db.completedShiftInvoices(32L, 1L)).thenReturn(List.of(invoice(101L, "5000.00", "BANK_TRANSFER")));

        assertEquals(new BigDecimal("20000.00"), service.summary("cashier").revenue());
        var next = service.summary("cashier");

        assertEquals(32L, next.shift().get("id"));
        assertEquals(new BigDecimal("5000.00"), next.revenue());
        assertEquals(0, next.cashRevenue().signum());
    }

    @Test
    void haiThuNganXemLienTiepKhongDungChungSoLieu() {
        when(db.account("other", true)).thenReturn(Optional.of(
                Map.of("id", 2L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.employee(2L)).thenReturn(Optional.of(Map.of("id", 11L)));
        when(db.latestShift(11L)).thenReturn(Optional.of(Map.of("id", 31L, "status", "OPEN")));
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "20000.00", "CASH")));
        when(db.completedShiftInvoices(31L, 2L)).thenReturn(List.of(invoice(101L, "5000.00", "BANK_TRANSFER")));

        var first = service.summary("cashier");
        var other = service.summary("other");

        assertEquals(new BigDecimal("20000.00"), first.revenue());
        assertEquals(new BigDecimal("5000.00"), other.revenue());
        assertEquals(101L, other.invoices().getFirst().id());
    }

    @Test
    void taiKhoanBiKhoaSauLanXemTruocBiTuChoiNgay() {
        service.summary("cashier");
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "status", "LOCKED", "role_code", "CASHIER")));

        assertThrows(AccessDeniedException.class, () -> service.summary("cashier"));
        verify(db, times(1)).completedShiftInvoices(30L, 1L);
    }

    @Test
    void doiVaiTroSauLanXemTruocBiTuChoi() {
        service.summary("cashier");
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "status", "ACTIVE", "role_code", "MANAGER")));

        assertThrows(AccessDeniedException.class, () -> service.summary("cashier"));
        verify(db, times(1)).completedShiftInvoices(30L, 1L);
    }

    @Test
    void nhieuHoaDonTrongCaVanDemVaCongDung() {
        List<ShiftInvoice> invoices = new ArrayList<>();
        for (int index = 0; index < 1000; index++) {
            invoices.add(invoice(index + 1L, "1000.00", index % 2 == 0 ? "CASH" : "BANK_TRANSFER"));
        }
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(invoices);

        var summary = service.summary("cashier");

        assertEquals(1000, summary.invoiceCount());
        assertEquals(new BigDecimal("1000000.00"), summary.revenue());
        assertEquals(new BigDecimal("500000.00"), summary.cashRevenue());
    }

    @Test
    void hoaDonQuaNuaDemVanThuocCaKhongBiLocTheoNgay() {
        var beforeMidnight = new ShiftInvoice(100L, "HD-100", "Thu ngân",
                Timestamp.from(Instant.parse("2026-11-24T16:59:59Z")), new BigDecimal("20000.00"), "CASH");
        var afterMidnight = new ShiftInvoice(101L, "HD-101", "Thu ngân",
                Timestamp.from(Instant.parse("2026-11-24T17:00:01Z")), new BigDecimal("30000.00"), "CASH");
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(beforeMidnight, afterMidnight));

        var summary = service.summary("cashier");

        assertEquals(2, summary.invoiceCount());
        assertEquals(new BigDecimal("50000.00"), summary.revenue());
    }

    @Test
    void loiDocTaiKhoanKhongTraTomTatThanhCongGia() {
        var failure = new DataAccessResourceFailureException("Không thể đọc tài khoản");
        when(db.account("cashier", true)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.summary("cashier")));
        verify(db, never()).employee(anyLong());
    }

    @Test
    void loiDocNhanVienKhongTraSoLieuKhong() {
        var failure = new DataAccessResourceFailureException("Không thể đọc nhân viên");
        when(db.employee(1L)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.summary("cashier")));
        verify(db, never()).latestShift(anyLong());
    }

    @Test
    void loiDocCaKhongTraSoLieuKhong() {
        var failure = new DataAccessResourceFailureException("Không thể đọc ca");
        when(db.latestShift(10L)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.summary("cashier")));
        verify(db, never()).completedShiftInvoices(anyLong(), anyLong());
    }

    @Test
    void loiDocHoaDonKhongBiCoiLaCaChuaCoGiaoDich() {
        var failure = new DataAccessResourceFailureException("Không thể đọc hóa đơn");
        when(db.completedShiftInvoices(30L, 1L)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.summary("cashier")));
    }

    private ShiftInvoice invoice(long id, String total, String method) {
        return new ShiftInvoice(id, "HD-" + id, "Thu ngân", (Timestamp) shift.get("opened_at"),
                new BigDecimal(total), method);
    }
}
