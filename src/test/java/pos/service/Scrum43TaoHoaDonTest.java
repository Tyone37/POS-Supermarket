package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class Scrum43TaoHoaDonTest {
    private PosRepository db;
    private SaleService service;
    private Map<String, Object> order;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service =
                new SaleService(
                        db, new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN"));
        when(db.account("cashier", true))
                .thenReturn(
                        Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        order =
                new HashMap<>(
                        Map.of(
                                "id",
                                10L,
                                "shift_id",
                                20L,
                                "status",
                                "DRAFT",
                                "subtotal_amount",
                                new BigDecimal("40000.00"),
                                "discount_amount",
                                BigDecimal.ZERO,
                                "total_amount",
                                new BigDecimal("40000.00")));
        when(db.order(10L, true)).thenReturn(Optional.of(order));
        when(db.shift(20L))
                .thenReturn(Optional.of(Map.of("id", 20L, "account_id", 1L, "status", "OPEN")));
        Map<String, Object> item =
                Map.of(
                        "id",
                        30L,
                        "product_id",
                        40L,
                        "quantity",
                        new BigDecimal("2"),
                        "unit_price",
                        new BigDecimal("20000.00"),
                        "discount_amount",
                        BigDecimal.ZERO,
                        "line_total",
                        new BigDecimal("40000.00"));
        when(db.items(10L)).thenReturn(List.of(item));
        when(db.product(40L, true))
                .thenReturn(
                        Optional.of(
                                Map.of(
                                        "id",
                                        40L,
                                        "name",
                                        "Bánh",
                                        "status",
                                        "ACTIVE",
                                        "stock_quantity",
                                        new BigDecimal("10"),
                                        "reserved_quantity",
                                        BigDecimal.ZERO,
                                        "quantity_scale",
                                        0)));
    }

    @Test
    void taoHoaDonSauThanhToanThanhCong() {
        when(db.createInvoice(anyLong(), anyLong(), anyLong(), anyString(), any())).thenReturn(50L);
        Forms.Payment form = new Forms.Payment("CASH", "50000", null, "pay-key-123", false);
        assertEquals(50L, service.pay("cashier", 10L, form));
        verify(db).createInvoice(eq(10L), anyLong(), eq(1L), startsWith("HD-"), any());
    }

    @Test
    void khongTaoHoaDonKhiKhachDuaThieuTien() {
        Forms.Payment form = new Forms.Payment("CASH", "30000", null, "pay-key-123", false);
        assertThrows(BusinessException.class, () -> service.pay("cashier", 10L, form));
        verify(db, never()).createInvoice(anyLong(), anyLong(), anyLong(), anyString(), any());
    }
}
