package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * US-KM-03: Hệ thống tự động áp dụng khuyến mãi khi bán hàng
 * Kiểm thử các tiêu chí chấp nhận AC1, AC2, AC3, AC4 và quy tắc nghiệp vụ.
 */
class UsKm03ApDungKhuyenMaiTest {
    private PosRepository db;
    private SaleService service;
    private Map<String, Object> order;
    private Map<String, Object> product;
    private Map<String, Object> activePromo;

    private Map<String, Object> createTestItem(long id, long orderId, long productId, String name,
                                               BigDecimal quantity, BigDecimal unitPrice,
                                               BigDecimal discount, BigDecimal total,
                                               Long promoId, String promoCode, String promoName) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("order_id", orderId);
        map.put("product_id", productId);
        map.put("product_name_snapshot", name);
        map.put("quantity", quantity);
        map.put("unit_price", unitPrice);
        map.put("discount_amount", discount);
        map.put("line_total", total);
        map.put("promotion_id", promoId);
        map.put("promotion_code_snapshot", promoCode);
        map.put("promotion_name_snapshot", promoName);
        return map;
    }

    @BeforeEach
    void setUp() {
        db = mock(PosRepository.class);
        service = new SaleService(db, new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN"));

        // Cashier account
        when(db.account("cashier", true)).thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));

        // Draft order with shift
        order = new HashMap<>(Map.of(
                "id", 100L,
                "shift_id", 200L,
                "status", "DRAFT",
                "subtotal_amount", BigDecimal.ZERO,
                "discount_amount", BigDecimal.ZERO,
                "total_amount", BigDecimal.ZERO,
                "order_no", "DH-20261124-001"
        ));
        when(db.order(100L, true)).thenReturn(Optional.of(order));
        when(db.shift(200L)).thenReturn(Optional.of(Map.of("id", 200L, "account_id", 1L, "status", "OPEN")));

        // Product: Bánh chocopie, giá 100.000 VNĐ
        product = new HashMap<>(Map.of(
                "id", 10L,
                "code", "SP001",
                "barcode", "893000000001",
                "name", "Bánh Chocopie",
                "sale_price", new BigDecimal("100000.00"),
                "stock_quantity", new BigDecimal("50"),
                "reserved_quantity", BigDecimal.ZERO,
                "status", "ACTIVE",
                "unit_code", "HOP",
                "quantity_scale", 0
        ));
        when(db.productsByCode("893000000001")).thenReturn(List.of(product));
        when(db.product(10L, true)).thenReturn(Optional.of(product));

        // Active promo: Giảm 20%
        activePromo = new HashMap<>(Map.of(
                "id", 500L,
                "code", "KM20",
                "name", "Khuyến mãi 20%",
                "discount_percent", new BigDecimal("20.00"),
                "lifecycle_status", "ENABLED",
                "starts_at", Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                "ends_at", Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS))
        ));
    }

    @Test
    @DisplayName("AC1: Khi thêm sản phẩm có chương trình khuyến mãi đang hiệu lực, tự động giảm 20% và tính tổng đơn đúng")
    void ac1_themSanPhamCoKhuyenMaiHieuLuc() {
        when(db.items(100L)).thenReturn(new ArrayList<>());
        when(db.activePromotionForProduct(eq(10L), any())).thenReturn(Optional.of(activePromo));

        service.add("cashier", 100L, "893000000001");

        // Đơn giá 100k, giảm 20% = 20k, lineTotal = 80k
        verify(db).createItem(
                eq(100L),
                eq(product),
                eq(BigDecimal.ONE),
                eq(new BigDecimal("20000.00")),
                eq(new BigDecimal("80000.00")),
                eq(500L),
                eq("KM20"),
                eq("Khuyến mãi 20%")
        );
    }

    @Test
    @DisplayName("AC2: Sản phẩm không có chương trình khuyến mãi còn hiệu lực (chưa bắt đầu/hết hạn/bị hủy) thì discount = 0")
    void ac2_themSanPhamKhongCoKhuyenMaiHieuLuc() {
        when(db.items(100L)).thenReturn(new ArrayList<>());
        when(db.activePromotionForProduct(eq(10L), any())).thenReturn(Optional.empty());

        service.add("cashier", 100L, "893000000001");

        // Giảm 0, lineTotal = 100k
        verify(db).createItem(
                eq(100L),
                eq(product),
                eq(BigDecimal.ONE),
                eq(BigDecimal.ZERO),
                eq(new BigDecimal("100000.00")),
                eq(null),
                eq(null),
                eq(null)
        );
    }

    @Test
    @DisplayName("AC3: Sau khi thanh toán, hóa đơn lưu giá và giảm giá; sửa/hủy CTKM không ảnh hưởng hóa đơn cũ")
    void ac3_thanhToanLuuGiaVaGiamGia() {
        // Giả lập đơn có 1 dòng đã giảm giá 20.000 VNĐ, cần thanh toán 80.000 VNĐ
        Map<String, Object> item = createTestItem(1001L, 100L, 10L, "Bánh Chocopie",
                BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("20000.00"), new BigDecimal("80000.00"),
                500L, "KM20", "Khuyến mãi 20%");

        order.put("subtotal_amount", new BigDecimal("100000.00"));
        order.put("discount_amount", new BigDecimal("20000.00"));
        order.put("total_amount", new BigDecimal("80000.00"));

        when(db.items(100L)).thenReturn(List.of(item));
        when(db.paymentByKey("cash-key-12345678")).thenReturn(Optional.empty());
        // Tại thời điểm thanh toán, KM vẫn hiệu lực như cũ
        when(db.activePromotionForProduct(eq(10L), any())).thenReturn(Optional.of(activePromo));

        when(db.createPayment(anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(888L);
        when(db.createInvoice(anyLong(), anyLong(), anyLong(), any(), any())).thenReturn(999L);

        Forms.Payment form = new Forms.Payment("CASH", "80000", null, "cash-key-12345678", null);
        long invoiceId = service.pay("cashier", 100L, form);

        assertEquals(999L, invoiceId);
        verify(db).markPaid(eq(100L), any());
        verify(db).createInvoice(eq(100L), eq(888L), eq(1L), any(), any());
    }

    @Test
    @DisplayName("AC4: Tại bước thanh toán, nếu chương trình đã hết hiệu lực, hệ thống tính lại giá, thông báo và yêu cầu Thu ngân xác nhận")
    void ac4_thanhToanKhiKhuyenMaiHetHieuLuc() {
        // Ban đầu lúc thêm vào giỏ có KM giảm 20.000 VNĐ
        Map<String, Object> item = createTestItem(1001L, 100L, 10L, "Bánh Chocopie",
                BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("20000.00"), new BigDecimal("80000.00"),
                500L, "KM20", "Khuyến mãi 20%");

        order.put("subtotal_amount", new BigDecimal("100000.00"));
        order.put("discount_amount", new BigDecimal("20000.00"));
        order.put("total_amount", new BigDecimal("80000.00"));

        when(db.items(100L)).thenReturn(List.of(item));
        when(db.paymentByKey("cash-key-12345678")).thenReturn(Optional.empty());

        // Lúc thanh toán, KM đã hết hạn / bị hủy -> activePromotionForProduct trả về empty
        when(db.activePromotionForProduct(eq(10L), any())).thenReturn(Optional.empty());

        Forms.Payment form = new Forms.Payment("CASH", "80000", null, "cash-key-12345678", null);

        // Hệ thống cập nhật lại dòng và ném BusinessException yêu cầu xác nhận tổng mới
        BusinessException ex = assertThrows(BusinessException.class, () -> service.pay("cashier", 100L, form));

        // Kiểm tra xem đã gọi updateItemPromotion về discount = 0 chưa
        verify(db).updateItemPromotion(
                eq(1001L),
                eq(BigDecimal.ONE),
                eq(BigDecimal.ZERO),
                eq(new BigDecimal("100000.00")),
                eq(null),
                eq(null),
                eq(null)
        );
        // Kiểm tra thông báo có chứa thông tin tổng mới
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Khuyến mãi đã thay đổi hiệu lực"));
        // Chưa được thanh toán
        verify(db, never()).markPaid(anyLong(), any());
    }

    @Test
    @DisplayName("Quy tắc nghiệp vụ: Mỗi sản phẩm chỉ áp dụng 1 khuyến mãi cao nhất tại 1 thời điểm")
    void quyTac_apDungMotKhuyenMaiCaoNhat() {
        when(db.items(100L)).thenReturn(new ArrayList<>());
        // activePromotionForProduct query có "order by p.discountPercent desc" -> trả về CTKM cao nhất
        when(db.activePromotionForProduct(eq(10L), any())).thenReturn(Optional.of(activePromo));

        service.add("cashier", 100L, "893000000001");

        verify(db).createItem(
                eq(100L),
                eq(product),
                eq(BigDecimal.ONE),
                eq(new BigDecimal("20000.00")),
                eq(new BigDecimal("80000.00")),
                eq(500L),
                eq("KM20"),
                eq("Khuyến mãi 20%")
        );
    }
}
