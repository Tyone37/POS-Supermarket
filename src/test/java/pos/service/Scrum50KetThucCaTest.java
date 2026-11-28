package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import pos.dto.ShiftClosePreview;
import pos.dto.ShiftInvoice;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum50KetThucCaTest {
    private static final Instant NOW = Instant.parse("2026-11-25T10:30:00.123456Z");
    private PosRepository db;
    private ShiftClosingService service;
    private Map<String, Object> shift;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new ShiftClosingService(db, Clock.fixed(NOW, ZoneOffset.UTC));
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "role_code", "CASHIER", "status", "ACTIVE")));
        when(db.employee(1L)).thenReturn(Optional.of(Map.of("id", 10L)));
        when(db.latestShift(10L)).thenReturn(Optional.of(Map.of("id", 30L, "status", "OPEN")));
        shift = new HashMap<>(Map.of("id", 30L, "account_id", 1L, "shift_no", "CA-30", "status", "OPEN",
                "opening_cash", new BigDecimal("500000.00"),
                "opened_at", Timestamp.from(Instant.parse("2026-11-25T01:00:00Z"))));
        when(db.shift(30L)).thenReturn(Optional.of(shift));
        when(db.completedCashRefunds(30L, 1L)).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void xemTruocChuaGhiDongCa() {
        var preview = service.preview("cashier", "500000");
        assertEquals(0, preview.invoiceCount());
        assertEquals(0, preview.revenue().signum());
        assertEquals(new BigDecimal("500000.00"), preview.expectedCash());
        assertEquals(shift.get("opened_at"), preview.openedAt());
        assertEquals(0, preview.difference().signum());
        verify(db, never()).closeShift(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"499000,-1000", "500000,0", "501000,1000", "0,-500000"})
    void tinhChenhLechAmDuongVaKhong(String actual, String difference) {
        var preview = service.preview("cashier", actual);
        assertEquals(0, new BigDecimal(difference).compareTo(preview.difference()));
        assertEquals(0, new BigDecimal(actual).compareTo(preview.actualCash()));
    }

    @ParameterizedTest
    @CsvSource({"CASH,100000.00,600000.00", "BANK_TRANSFER,100000.00,500000.00", "CASH,0.00,500000.00"})
    void tinhTheoPhuongThucSauGiam(String method, String total, String expected) {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, total, method)));
        var preview = service.preview("cashier", "600000");
        assertEquals(1, preview.invoiceCount());
        assertEquals(new BigDecimal(total), preview.revenue());
        assertEquals(new BigDecimal(expected), preview.expectedCash());
    }

    @Test
    void tienMatVaChuyenKhoanChiCongTienMatVaoDuKien() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "77600.00", "CASH"), invoice(101L, "24000.00", "BANK_TRANSFER")));
        var preview = service.preview("cashier", "577600");
        assertEquals(new BigDecimal("101600.00"), preview.revenue());
        assertEquals(new BigDecimal("77600.00"), preview.cashSales());
        assertEquals(new BigDecimal("577600.00"), preview.expectedCash());
    }

    @Test
    void truHoanTienMatNhungKhongTruDoanhThuBanHang() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "100000.00", "CASH")));
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("40000.00"));
        var preview = service.preview("cashier", "560000");
        assertEquals(new BigDecimal("40000.00"), preview.cashRefunds());
        assertEquals(new BigDecimal("560000.00"), preview.expectedCash());
        assertEquals(new BigDecimal("100000.00"), preview.revenue());
        assertEquals(0, preview.difference().signum());
    }

    @Test
    void caKhongBanNhungCoHoanTienTruTuTienDauCa() {
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("100000.00"));
        var preview = service.preview("cashier", "400000");
        assertEquals(0, preview.revenue().signum());
        assertEquals(new BigDecimal("400000.00"), preview.expectedCash());
    }

    @Test
    void hoanHetTienTrongNganKeoChoPhepDuKienKhong() {
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("500000.00"));
        assertEquals(0, service.preview("cashier", "0").expectedCash().signum());
    }

    @Test
    void layKhoanHoanTheoDungCaVaThuNgan() {
        service.preview("cashier", "0");
        verify(db).completedCashRefunds(30L, 1L);
        verify(db, never()).completedCashRefunds(29L, 1L);
        verify(db, never()).completedCashRefunds(30L, 2L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "-1", "abc", "12abc", "1.5", "NaN", "Infinity", "10000000000000000"})
    void tuChoiTienThucTeKhongHopLe(String actual) {
        assertThrows(BusinessException.class, () -> service.preview("cashier", actual));
        verify(db, never()).closeShift(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "9999999999999999"})
    void chapNhanTienThucTeTaiBien(String actual) {
        assertEquals(0, new BigDecimal(actual).compareTo(service.preview("cashier", actual).actualCash()));
    }

    @Test
    void luuSoLieuVaGioMayChuChinhXacMiliGiay() {
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("100000.00"));
        var reviewed = service.preview("cashier", "390000");
        service.close("cashier", reviewed);
        verify(db).closeShift(reviewed, Timestamp.from(Instant.parse("2026-11-25T10:30:00.123Z")));
        assertEquals(new BigDecimal("-10000.00"), reviewed.difference());
    }

    @Test
    void kiemTraKhoaVaDocLaiTruocKhiLuu() {
        var reviewed = service.preview("cashier", "500000");
        clearInvocations(db);
        service.close("cashier", reviewed);
        var sequence = inOrder(db);
        sequence.verify(db).account("cashier", true);
        sequence.verify(db).employee(1L);
        sequence.verify(db).latestShift(10L);
        sequence.verify(db).shift(30L);
        sequence.verify(db).completedShiftInvoices(30L, 1L);
        sequence.verify(db).completedCashRefunds(30L, 1L);
        sequence.verify(db).closeShift(any(), any());
    }

    @Test
    void doanhThuThayDoiPhaiXacNhanLai() {
        var reviewed = service.preview("cashier", "500000");
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "10000.00", "CASH")));
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void hoanTienPhatSinhPhaiXacNhanLai() {
        var reviewed = service.preview("cashier", "500000");
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("10000.00"));
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void soHoaDonTangCungTongTienVanPhaiXacNhanLai() {
        var reviewed = service.preview("cashier", "500000");
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "0.00", "CASH")));
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
    }

    @Test
    void doiPhuongThucPhaiXacNhanLai() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "10000.00", "CASH")));
        var reviewed = service.preview("cashier", "510000");
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "10000.00", "BANK_TRANSFER")));
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
    }

    @Test
    void tienDauCaThayDoiPhaiXacNhanLai() {
        var reviewed = service.preview("cashier", "500000");
        shift.put("opening_cash", new BigDecimal("600000.00"));
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
    }

    @Test
    void khongCoCaMoKhongTaoHoacSuaDuLieu() {
        when(db.latestShift(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
        assertThrows(BusinessException.class, () -> service.close("cashier", null));
        verify(db, never()).closeShift(any(), any());
        verify(db, never()).createShift(any(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void caDongSauKhiChoKhoaBiTuChoi() {
        shift.put("status", "CLOSED");
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void caKhongTonTaiBiTuChoi() {
        when(db.shift(30L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
    }

    @Test
    void caKhacChuBiTuChoi() {
        shift.put("account_id", 2L);
        assertThrows(AccessDeniedException.class, () -> service.preview("cashier", "0"));
        verify(db, never()).closeShift(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void vaiTroKhacBiTuChoi(String role) {
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "role_code", role, "status", "ACTIVE")));
        assertThrows(AccessDeniedException.class, () -> service.close("cashier", null));
        verify(db, never()).shift(anyLong());
    }

    @Test
    void taiKhoanKhoaBiTuChoi() {
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "role_code", "CASHIER", "status", "LOCKED")));
        assertThrows(AccessDeniedException.class, () -> service.close("cashier", null));
    }

    @Test
    void taiKhoanKhongTonTaiBiTuChoi() {
        assertThrows(AccessDeniedException.class, () -> service.preview("missing", "0"));
    }

    @Test
    void thieuNhanVienBiTuChoi() {
        when(db.employee(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
    }

    @Test
    void chuaCoBanXacNhanBiTuChoi() {
        assertThrows(BusinessException.class, () -> service.close("cashier", null));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void banXacNhanCuKhongDuocDongCaMoi() {
        var reviewed = service.preview("cashier", "500000");
        shift.put("id", 31L);
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void guiLapSauKhiDongKhongGhiLanHai() {
        var reviewed = service.preview("cashier", "500000");
        service.close("cashier", reviewed);
        shift.put("status", "CLOSED");
        assertThrows(BusinessException.class, () -> service.close("cashier", reviewed));
        verify(db).closeShift(any(), any());
    }

    @Test
    void gioKetThucKhongDuocTruocGioMo() {
        var reviewed = service.preview("cashier", "500000");
        var early = new ShiftClosingService(db, Clock.fixed(Instant.parse("2026-11-24T00:00:00Z"), ZoneOffset.UTC));
        assertThrows(BusinessException.class, () -> early.close("cashier", reviewed));
        verify(db, never()).closeShift(any(), any());
    }

    @Test
    void duKienVuotGioiHanBiTuChoi() {
        shift.put("opening_cash", new BigDecimal("9999999999999999.00"));
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(invoice(100L, "1.00", "CASH")));
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
    }

    @Test
    void doanhThuChuyenKhoanVuotGioiHanBiTuChoi() {
        when(db.completedShiftInvoices(30L, 1L)).thenReturn(List.of(
                invoice(100L, "9999999999999999.00", "BANK_TRANSFER"), invoice(101L, "1.00", "BANK_TRANSFER")));
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
    }

    @Test
    void hoanTienVuotTienTrongNganKeoBiTuChoi() {
        when(db.completedCashRefunds(30L, 1L)).thenReturn(new BigDecimal("500001.00"));
        assertThrows(BusinessException.class, () -> service.preview("cashier", "0"));
    }

    @Test
    void loiDocHoanTienKhongDuocCoiLaKhongHoanTien() {
        var failure = new DataAccessResourceFailureException("Không thể đọc hoàn tiền");
        when(db.completedCashRefunds(30L, 1L)).thenThrow(failure);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.preview("cashier", "0")));
    }

    @Test
    void loiLuuKhongBaoThanhCong() {
        var reviewed = service.preview("cashier", "500000");
        var failure = new DataAccessResourceFailureException("Không thể lưu ca");
        doThrow(failure).when(db).closeShift(any(), any());
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.close("cashier", reviewed)));
    }

    private ShiftInvoice invoice(long id, String total, String method) {
        return new ShiftInvoice(id, "HD-" + id, "Thu ngân", (Timestamp) shift.get("opened_at"),
                new BigDecimal(total), method);
    }
}
