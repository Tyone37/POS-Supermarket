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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum51KhoaCaTest {
    private PosRepository db;
    private SaleService sales;
    private ShiftClosingService closing;
    private Map<String, Object> shift;
    private Map<String, Object> order;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        sales = new SaleService(db, new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN"));
        closing = new ShiftClosingService(db, Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC));
        when(db.account("cashier", true)).thenReturn(Optional.of(
                Map.of("id", 1L, "role_code", "CASHIER", "status", "ACTIVE")));
        when(db.employee(1L)).thenReturn(Optional.of(Map.of("id", 11L)));
        shift = new HashMap<>(Map.of("id", 20L, "account_id", 1L, "status", "OPEN", "shift_no", "CA-20",
                "opening_cash", new BigDecimal("500000.00"),
                "opened_at", Timestamp.from(Instant.parse("2026-11-25T01:00:00Z"))));
        when(db.shift(20L)).thenReturn(Optional.of(shift));
        when(db.latestShift(11L)).thenReturn(Optional.of(shift));
        when(db.completedCashRefunds(20L, 1L)).thenReturn(BigDecimal.ZERO);
        order = new HashMap<>(Map.of("id", 10L, "shift_id", 20L, "status", "DRAFT",
                "subtotal_amount", new BigDecimal("40000.00"), "discount_amount", BigDecimal.ZERO,
                "total_amount", new BigDecimal("40000.00")));
        when(db.order(10L, true)).thenReturn(Optional.of(order));
        when(db.items(10L)).thenReturn(List.of(Map.of("id", 30L, "product_id", 40L,
                "quantity", new BigDecimal("2"), "unit_price", new BigDecimal("20000.00"),
                "discount_amount", BigDecimal.ZERO, "line_total", new BigDecimal("40000.00"))));
        when(db.product(40L, true)).thenReturn(Optional.of(Map.of("id", 40L, "status", "ACTIVE", "name", "Sữa",
                "stock_quantity", new BigDecimal("10"), "reserved_quantity", BigDecimal.ZERO, "quantity_scale", 0)));
    }

    static Stream<Arguments> closedPayments() {
        return Stream.of("DRAFT", "PENDING", "PAID", "CANCELLED")
                .flatMap(status -> Stream.of("CASH", "BANK_TRANSFER").map(method -> Arguments.of(status, method)));
    }

    @ParameterizedTest
    @MethodSource("closedPayments")
    void thanhToanCaDongBiTuChoiTruocKhiChamTon(String status, String method) {
        shift.put("status", "CLOSED");
        order.put("status", status);

        var error = assertThrows(BusinessException.class, () -> sales.pay("cashier", 10L, payment(method)));

        assertTrue(error.getMessage().contains("Ca đã kết thúc"));
        assertNoPaymentWrites();
        verify(db, never()).product(anyLong(), anyBoolean());
    }

    @Test
    void replayThanhToanThanhCongSauDongCaVanBiTuChoi() {
        shift.put("status", "CLOSED");
        order.put("status", "PAID");
        stubSuccessfulReplay();

        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10L, payment("CASH")));

        verify(db, never()).paymentByKey(anyString());
        assertNoPaymentWrites();
    }

    @Test
    void replayCaMoVanTraHoaDonCuKhongTruKhoLanHai() {
        order.put("status", "PAID");
        stubSuccessfulReplay();

        assertEquals(51L, sales.pay("cashier", 10L, payment("CASH")));
        assertNoPaymentWrites();
    }

    @Test
    void thanhToanCaMoVanHoatDong() {
        when(db.createInvoice(anyLong(), anyLong(), anyLong(), anyString(), any())).thenReturn(51L);

        assertEquals(51L, sales.pay("cashier", 10L, payment("CASH")));
        verify(db).deductStock(anyLong(), any(), any());
        verify(db).markPaid(anyLong(), any());
        verify(db).createInvoice(anyLong(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void caDongTrongLucChoKhoaKhongDuocThanhToan() {
        var closed = new HashMap<>(shift);
        closed.put("status", "CLOSED");
        when(db.shift(20L)).thenReturn(Optional.of(shift)).thenReturn(Optional.of(closed));

        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10L, payment("CASH")));
        assertNoPaymentWrites();
    }

    @Test
    void coCaMoiKhongDuocThanhToanDonCuaCaCu() {
        shift.put("status", "CLOSED");
        when(db.latestShift(11L)).thenReturn(Optional.of(Map.of("id", 21L, "status", "OPEN")));

        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10L, payment("CASH")));
        assertNoPaymentWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
    void khongGhiLanThanhToanThatBaiVaoCaDong(String method) {
        shift.put("status", "CLOSED");
        when(db.paymentByKey("failed-key-123")).thenReturn(Optional.of(
                Map.of("id", 50L, "order_id", 10L, "status", "FAILED", "method", method)));

        assertThrows(BusinessException.class, () -> sales.fail("cashier", 10L, method, "failed-key-123"));
        assertNoPaymentWrites();
    }

    @Test
    void khongTaoDonMoiKhiCaDaDong() {
        shift.put("status", "CLOSED");

        assertThrows(BusinessException.class, () -> sales.create("cashier", "create-key-123"));
        verify(db, never()).createOrder(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void replayTaoDonCuSauDongCaBiTuChoi() {
        shift.put("status", "CLOSED");
        when(db.orderByKey("create-key-123")).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> sales.create("cashier", "create-key-123"));
        verify(db, never()).createOrder(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void replayTaoDonCaMoVanTraDungDon() {
        when(db.orderByKey("create-key-123")).thenReturn(Optional.of(order));

        assertEquals(10L, sales.create("cashier", "create-key-123"));
        verify(db, never()).createOrder(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void caChuaMoKhongTaoDon() {
        when(db.latestShift(11L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> sales.create("cashier", "create-key-123"));
        verify(db, never()).createOrder(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void moCaMoiChoPhepTaoDonMoi() {
        when(db.createOrder(anyLong(), anyString(), anyString(), any())).thenReturn(12L);

        assertEquals(12L, sales.create("cashier", "create-key-123"));
        verify(db).createOrder(anyLong(), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "quantity", "remove", "cancel"})
    void caDongKhongDuocThayDoiDon(String action) {
        shift.put("status", "CLOSED");

        assertThrows(BusinessException.class, () -> {
            switch (action) {
                case "add" -> sales.add("cashier", 10L, "SP001");
                case "quantity" -> sales.quantity("cashier", 10L, 30L, "3", false);
                case "remove" -> sales.quantity("cashier", 10L, 30L, "0", true);
                case "cancel" -> sales.cancel("cashier", 10L, "Khách đổi ý", true);
                default -> throw new AssertionError("Unexpected action");
            }
        });
        verify(db, never()).createItem(anyLong(), any());
        verify(db, never()).quantity(anyLong(), any(), any());
        verify(db, never()).removeItem(anyLong());
        verify(db, never()).cancel(anyLong(), anyString(), any());
        assertNoPaymentWrites();
    }

    @Test
    void xemDonCuChiDocKhongMoLaiCa() {
        shift.put("status", "CLOSED");

        assertEquals(false, sales.view("cashier", 10L).get("shift_open"));
        assertEquals("CLOSED", shift.get("status"));
        assertNoPaymentWrites();
    }

    @Test
    void khongDuocTruyCapDonCuaThuNganKhac() {
        shift.put("account_id", 2L);

        assertThrows(AccessDeniedException.class, () -> sales.pay("cashier", 10L, payment("CASH")));
        assertNoPaymentWrites();
    }

    @Test
    void dongCaRoiThanhToanMuonKhongTruKhoVaKhongGhiDongLanHai() {
        var reviewed = closing.preview("cashier", "500000");
        doAnswer(invocation -> {
            shift.put("status", "CLOSED");
            shift.put("closed_at", invocation.getArgument(1));
            shift.put("closing_actual_cash", reviewed.actualCash());
            shift.put("closing_difference", reviewed.difference());
            return null;
        }).when(db).closeShift(any(), any());
        closing.close("cashier", reviewed);
        var saved = new HashMap<>(shift);

        assertThrows(BusinessException.class, () -> sales.pay("cashier", 10L, payment("CASH")));
        assertThrows(BusinessException.class, () -> closing.close("cashier", reviewed));

        assertEquals(saved, shift);
        verify(db, times(1)).closeShift(any(), any());
        assertNoPaymentWrites();
    }

    private Forms.Payment payment(String method) {
        return new Forms.Payment(method, "50000", "BANK_TRANSFER".equals(method) ? "FT123" : null,
                "payment-key-123", true);
    }

    private void stubSuccessfulReplay() {
        var payment = new HashMap<String, Object>(Map.of("id", 50L, "order_id", 10L, "status", "SUCCESS",
                "method", "CASH", "amount_received", new BigDecimal("50000.00")));
        payment.put("bank_reference", null);
        when(db.paymentByKey("payment-key-123")).thenReturn(Optional.of(payment));
        when(db.invoiceForOrder(10L)).thenReturn(Optional.of(Map.of("id", 51L)));
    }

    private void assertNoPaymentWrites() {
        verify(db, never()).deductStock(anyLong(), any(), any());
        verify(db, never()).movement(any(), any());
        verify(db, never()).markPaid(anyLong(), any());
        verify(db, never()).createInvoice(anyLong(), anyLong(), anyLong(), anyString(), any());
        verify(db, never()).createPayment(anyLong(), anyString(), anyString(), any(), any(), any(),
                anyString(), any(), any(), any());
    }
}
