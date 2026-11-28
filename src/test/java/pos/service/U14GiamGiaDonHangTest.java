package pos.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

/**
 * U1.4: Áp dụng khuyến mãi, giảm giá cho đơn hàng
 * Kiểm thử AC1, AC2, AC3, AC5, AC6 và phân bổ làm tròn VND.
 */
class U14GiamGiaDonHangTest {
    private PosRepository db;
    private PasswordEncoder encoder;
    private SaleService service;
    private Map<String, Object> order;
    private Map<String, Object> item1;
    private Map<String, Object> item2;

    private Map<String, Object> makeItem(long id, long productId, String name, BigDecimal qty, BigDecimal price,
                                         BigDecimal discount, Long promoId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("order_id", 10L);
        map.put("product_id", productId);
        map.put("product_name_snapshot", name);
        map.put("quantity", qty);
        map.put("unit_price", price);
        map.put("discount_amount", discount);
        map.put("line_total", qty.multiply(price).subtract(discount).setScale(2));
        map.put("promotion_id", promoId);
        return map;
    }

    @BeforeEach
    void setUp() {
        db = mock(PosRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new SaleService(db, new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN"), encoder);

        when(db.account("cashier", true))
                .thenReturn(Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.account("manager", true))
                .thenReturn(Optional.of(Map.of(
                        "id", 2L, "status", "ACTIVE", "role_code", "MANAGER",
                        "password_hash", encoder.encode("demo123"))));

        order = new HashMap<>(Map.of(
                "id", 10L,
                "shift_id", 20L,
                "status", "DRAFT",
                "subtotal_amount", new BigDecimal("100000.00"),
                "discount_amount", BigDecimal.ZERO,
                "total_amount", new BigDecimal("100000.00"),
                "discount_mode", "NONE"
        ));
        when(db.order(10L, true)).thenReturn(Optional.of(order));
        when(db.shift(20L)).thenReturn(Optional.of(Map.of("id", 20L, "account_id", 1L, "status", "OPEN")));

        // item1: 30,000; item2: 70,000 -> total 100,000
        item1 = makeItem(101L, 1L, "Sữa", BigDecimal.ONE, new BigDecimal("30000.00"), BigDecimal.ZERO, null);
        item2 = makeItem(102L, 2L, "Bánh", BigDecimal.ONE, new BigDecimal("70000.00"), BigDecimal.ZERO, null);
        when(db.items(10L)).thenReturn(List.of(item1, item2));
    }

    @Test
    @DisplayName("AC1: Nhập coupon hợp lệ -> phân bổ theo tỷ trọng, phần dư gán dòng cuối")
    void ac1_apDungCouponHopLeVaPhanBo() {
        // Coupon giảm 10.001 VNĐ trên đơn 100.000 (30k + 70k):
        // item1: 10001 * 30000 / 100000 = 3000.3 -> 3000
        // item2 (dòng cuối): 10001 - 3000 = 7001
        Map<String, Object> coupon = Map.of(
                "id", 5L,
                "code", "VIP10",
                "name", "Giảm 10.001đ",
                "discount_type", "AMOUNT",
                "discount_value", new BigDecimal("10001.00"),
                "min_order_amount", BigDecimal.ZERO,
                "starts_at", Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                "ends_at", Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)),
                "lifecycle_status", "ENABLED"
        );
        when(db.couponByCode("VIP10")).thenReturn(Optional.of(coupon));

        service.applyCoupon("cashier", 10L, "vip10", false);

        verify(db).updateItemPromotion(eq(101L), eq(BigDecimal.ONE), eq(new BigDecimal("3000.00")),
                eq(new BigDecimal("27000.00")), eq(null), eq(null), eq(null));
        verify(db).updateItemPromotion(eq(102L), eq(BigDecimal.ONE), eq(new BigDecimal("7001.00")),
                eq(new BigDecimal("62999.00")), eq(null), eq(null), eq(null));
        verify(db).orderDiscount(10L, "COUPON", 5L, "VIP10", null);
    }

    @Test
    @DisplayName("AC1: Coupon hết hạn hoặc không tồn tại -> từ chối và không thay đổi đơn")
    void ac1_couponHetHanBiTuChoi() {
        Map<String, Object> expired = Map.of(
                "id", 6L, "code", "OLD", "name", "Hết hạn",
                "discount_type", "PERCENT", "discount_value", new BigDecimal("10"),
                "min_order_amount", BigDecimal.ZERO,
                "starts_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                "ends_at", Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                "lifecycle_status", "ENABLED"
        );
        when(db.couponByCode("OLD")).thenReturn(Optional.of(expired));

        assertThrows(BusinessException.class, () -> service.applyCoupon("cashier", 10L, "OLD", false));
        assertThrows(BusinessException.class, () -> service.applyCoupon("cashier", 10L, "UNKNOWN", false));
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC2: Giảm giá trực tiếp trong hạn mức 10% (theo dòng hoặc toàn đơn) -> cập nhật thành công")
    void ac2_giamTrucTiepTrongHanMuc() {
        // 10% của 100.000 = 10.000 (nằm trong hạn mức thu ngân)
        Forms.DirectDiscount form = new Forms.DirectDiscount("ORDER", null, "PERCENT", "10", false, null, null);
        service.applyDirectDiscount("cashier", 10L, form);

        verify(db).updateItemPromotion(eq(101L), eq(BigDecimal.ONE), eq(new BigDecimal("3000.00")),
                eq(new BigDecimal("27000.00")), eq(null), eq(null), eq(null));
        verify(db).updateItemPromotion(eq(102L), eq(BigDecimal.ONE), eq(new BigDecimal("7000.00")),
                eq(new BigDecimal("63000.00")), eq(null), eq(null), eq(null));
        verify(db).orderDiscount(10L, "DIRECT_ORDER", null, null, null);
    }

    @Test
    void boTrongCheckboxVaThongTinQuanLyVanGiamToanDonTrongHanMuc() {
        var form = new Forms.DirectDiscount("ORDER", null, "PERCENT", "5", null, "", "");
        service.applyDirectDiscount("cashier", 10L, form);
        verify(db).orderDiscount(10L, "DIRECT_ORDER", null, null, null);
        verify(db).updateItemPromotion(eq(101L), eq(BigDecimal.ONE), eq(new BigDecimal("1500.00")),
                eq(new BigDecimal("28500.00")), eq(null), eq(null), eq(null));
        verify(db).updateItemPromotion(eq(102L), eq(BigDecimal.ONE), eq(new BigDecimal("3500.00")),
                eq(new BigDecimal("66500.00")), eq(null), eq(null), eq(null));
    }

    @Test
    void khongGuiCheckboxKhongDuocTuDongThayTheUuDaiCu() {
        item1.put("promotion_id", 500L);
        item1.put("discount_amount", new BigDecimal("5000.00"));
        var form = new Forms.DirectDiscount("ORDER", null, "PERCENT", "5", null, "", "");
        assertThrows(BusinessException.class, () -> service.applyDirectDiscount("cashier", 10L, form));
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any());
    }

    @Test
    void boTrongMucGiamKhongCapNhatDon() {
        var form = new Forms.DirectDiscount("ORDER", null, "PERCENT", "", null, "", "");
        assertThrows(BusinessException.class, () -> service.applyDirectDiscount("cashier", 10L, form));
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC3: Vượt hạn mức 10% của Thu ngân -> yêu cầu Quản lý ca phê duyệt")
    void ac3_vuotHanMucCanQuanLyPheDuyet() {
        // Giảm 20% > 10% -> chưa có QL duyệt sẽ bị chặn
        Forms.DirectDiscount noApproval = new Forms.DirectDiscount("ORDER", null, "PERCENT", "20", false, null, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.applyDirectDiscount("cashier", 10L, noApproval));
        assertTrue(ex.getMessage().contains("10%"));

        // Sai mật khẩu QL -> chặn
        Forms.DirectDiscount wrongPwd = new Forms.DirectDiscount("ORDER", null, "PERCENT", "20", false, "manager", "wrong");
        assertThrows(BusinessException.class, () -> service.applyDirectDiscount("cashier", 10L, wrongPwd));

        // Đúng QL duyệt -> áp dụng thành công và lưu approved_by = 2L
        Forms.DirectDiscount approved = new Forms.DirectDiscount("ORDER", null, "PERCENT", "20", false, "manager", "demo123");
        service.applyDirectDiscount("cashier", 10L, approved);
        verify(db).orderDiscount(10L, "DIRECT_ORDER", null, null, 2L);
    }

    @Test
    @DisplayName("AC5: Giá trị giảm không hợp lệ hoặc làm phải trả âm -> chặn áp dụng")
    void ac5_giaTriGiamKhongHopLe() {
        // Giảm > 100%
        assertThrows(BusinessException.class, () -> service.applyDirectDiscount("cashier", 10L,
                new Forms.DirectDiscount("ORDER", null, "PERCENT", "105", false, "manager", "demo123")));
        // Giảm số tiền vượt giá trị dòng (item1 chỉ có 30.000)
        assertThrows(BusinessException.class, () -> service.applyDirectDiscount("cashier", 10L,
                new Forms.DirectDiscount("ITEM", 101L, "AMOUNT", "35000", false, "manager", "demo123")));
        verify(db, never()).orderDiscount(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC6: Đổi từ khuyến mãi tự động sang coupon/giảm trực tiếp cần xác nhận thay thế, không cộng dồn")
    void ac6_doiHinhThucUuDaiCanXacNhanThayThe() {
        // item1 đang có khuyến mãi tự động
        item1.put("promotion_id", 500L);
        item1.put("discount_amount", new BigDecimal("5000.00"));

        Map<String, Object> coupon = Map.of(
                "id", 5L, "code", "VIP10", "name", "Giảm 10%",
                "discount_type", "PERCENT", "discount_value", new BigDecimal("10"),
                "min_order_amount", BigDecimal.ZERO,
                "starts_at", Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                "ends_at", Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)),
                "lifecycle_status", "ENABLED"
        );
        when(db.couponByCode("VIP10")).thenReturn(Optional.of(coupon));

        // Chưa xác nhận thay thế -> báo lỗi yêu cầu xác nhận
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.applyCoupon("cashier", 10L, "VIP10", false));
        assertTrue(ex.getMessage().contains("xác nhận thay thế"));

        // Đã xác nhận -> xóa KM tự động cũ, áp dụng coupon mới (không cộng dồn)
        service.applyCoupon("cashier", 10L, "VIP10", true);
        verify(db).orderDiscount(10L, "COUPON", 5L, "VIP10", null);
    }
}
