package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import pos.config.TransferSettings;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class Scrum44XemHoaDonTest {
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
    void xemHoaDonCuaMinh() {
        Map<String, Object> invoice =
                new HashMap<>(Map.of("id", 50L, "order_id", 10L, "issued_by", 1L));
        when(db.invoice(50L)).thenReturn(Optional.of(invoice));
        Map<String, Object> result = service.invoice("cashier", 50L);
        assertEquals(50L, result.get("id"));
        assertEquals(db.items(10L), result.get("items"));
    }

    @Test
    void xemHoaDonKhongTonTai() {
        assertThrows(BusinessException.class, () -> service.invoice("cashier", 999L));
    }

    @Test
    void khongDuocXemHoaDonCuaThuNganKhac() {
        when(db.invoice(50L))
                .thenReturn(Optional.of(Map.of("id", 50L, "order_id", 10L, "issued_by", 99L)));
        assertThrows(AccessDeniedException.class, () -> service.invoice("cashier", 50L));
    }
}
