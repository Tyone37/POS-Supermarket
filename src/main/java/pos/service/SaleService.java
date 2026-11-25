package pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class SaleService {
    private static final BigDecimal CASHIER_DISCOUNT_LIMIT_RATE = new BigDecimal("0.10");
    private final PosRepository db;
    private final TransferSettings transfer;
    private final PasswordEncoder passwordEncoder;

    public SaleService(PosRepository db, TransferSettings transfer) {
        this(db, transfer, new BCryptPasswordEncoder());
    }

    @Autowired
    public SaleService(PosRepository db, TransferSettings transfer, PasswordEncoder passwordEncoder) {
        this.db = db;
        this.transfer = transfer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public List<Map<String, Object>> list(String username) {
        return db.orders(PosSupport.id(PosSupport.actor(db, username, "CASHIER"), "id"));
    }

    @Transactional
    public long create(String username, String requestKey) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        String key = PosSupport.requestKey(requestKey);
        var replay = db.orderByKey(key);
        if (replay.isPresent()) {
            ownedOrder(username, PosSupport.id(replay.get(), "id"));
            return PosSupport.id(replay.get(), "id");
        }
        long employee = PosSupport.id(db.employee(PosSupport.id(actor, "id"))
                .orElseThrow(() -> new BusinessException("Tài khoản chưa có hồ sơ nhân viên.")), "id");
        var current = db.latestShift(employee).orElseThrow(() -> new BusinessException("Vui lòng mở ca trước khi bán hàng."));
        var shift = db.shift(PosSupport.id(current, "id")).orElseThrow();
        requireOpen(shift);
        return db.createOrder(PosSupport.id(shift, "id"), PosSupport.number("DH-"), key, PosSupport.now());
    }

    private Map<String, Object> ownedOrder(String username, long id) {
        var actor = PosSupport.actor(db, username, "CASHIER");
        var order = db.order(id, true).orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại."));
        var shift = db.shift(PosSupport.id(order, "shift_id")).orElseThrow();
        if (PosSupport.id(shift, "account_id") != PosSupport.id(actor, "id")) {
            throw new AccessDeniedException("Không được truy cập đơn hàng của thu ngân khác.");
        }
        return order;
    }

    private void requireOpen(Map<String, Object> shift) {
        if (!"OPEN".equals(shift.get("status"))) {
            throw new BusinessException("Ca đã kết thúc. Vui lòng mở ca mới.");
        }
    }

    private void editable(Map<String, Object> order) {
        requireOpen(db.shift(PosSupport.id(order, "shift_id")).orElseThrow());
        if (!"DRAFT".equals(order.get("status"))) {
            throw new BusinessException("Chỉ được thay đổi hoặc thanh toán đơn đang lập.");
        }
    }

    @Transactional
    public Map<String, Object> view(String username, long id) {
        var order = ownedOrder(username, id);
        var shift = db.shift(PosSupport.id(order, "shift_id")).orElseThrow();
        order.put("shift_open", "OPEN".equals(shift.get("status")));
        return order;
    }

    @Transactional
    public List<Map<String, Object>> items(String username, long order) {
        ownedOrder(username, order);
        return db.items(order);
    }

    @Transactional
    public List<Map<String, Object>> payments(String username, long order) {
        ownedOrder(username, order);
        return db.payments(order);
    }

    @Transactional
    public Long invoiceId(String username, long order) {
        ownedOrder(username, order);
        return db.invoiceForOrder(order).map(row -> PosSupport.id(row, "id")).orElse(null);
    }

    @Transactional
    public void add(String username, long id, String code) {
        var order = ownedOrder(username, id);
        editable(order);
        var matches = db.productsByCode(PosSupport.text(code, "Mã sản phẩm", 50));
        if (matches.isEmpty()) {
            throw new BusinessException("Không tìm thấy mã sản phẩm đang bán.");
        }
        if (matches.size() > 1) {
            throw new BusinessException("Mã vạch/SKU khớp nhiều sản phẩm. Vui lòng kiểm tra danh mục.");
        }
        var found = matches.getFirst();
        var product = db.product(PosSupport.id(found, "id"), true).orElseThrow();
        if (!"ACTIVE".equals(product.get("status"))) {
            throw new BusinessException("Sản phẩm đã ngừng bán.");
        }
        var currentItems = db.items(id);
        boolean autoPromoAllowed = isAutoPromoAllowed(order, currentItems);
        var existing = currentItems.stream().filter(row -> PosSupport.id(row, "product_id") == PosSupport.id(product, "id")).findFirst();
        if (existing.isPresent()) {
            var item = existing.get();
            updateQuantity(item, product, ((BigDecimal) item.get("quantity")).add(BigDecimal.ONE), autoPromoAllowed);
        } else {
            available(product, BigDecimal.ONE);
            long productId = PosSupport.id(product, "id");
            var promoOpt = autoPromoAllowed ? db.activePromotionForProduct(productId, PosSupport.now()) : java.util.Optional.<Map<String, Object>>empty();
            BigDecimal unitPrice = (BigDecimal) product.get("sale_price");
            BigDecimal gross = unitPrice.multiply(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount = BigDecimal.ZERO;
            Long promoId = null;
            String promoCode = null;
            String promoName = null;

            if (promoOpt.isPresent()) {
                var promo = promoOpt.get();
                promoId = PosSupport.id(promo, "id");
                promoCode = (String) promo.get("code");
                promoName = (String) promo.get("name");
                BigDecimal percent = (BigDecimal) promo.get("discount_percent");
                discount = gross.multiply(percent).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).setScale(2);
            }
            BigDecimal lineTotal = gross.subtract(discount);
            db.createItem(id, product, BigDecimal.ONE, discount, lineTotal, promoId, promoCode, promoName);
        }
        recalculate(id);
    }

    private void available(Map<String, Object> product, BigDecimal quantity) {
        BigDecimal stock = ((BigDecimal) product.get("stock_quantity")).subtract((BigDecimal) product.get("reserved_quantity"));
        if (stock.compareTo(quantity) < 0) {
            throw new BusinessException("Tồn kho khả dụng không đủ cho sản phẩm " + product.get("name") + ".");
        }
    }

    private void updateQuantity(Map<String, Object> item, Map<String, Object> product, BigDecimal quantity, boolean autoPromoAllowed) {
        available(product, quantity);
        BigDecimal unitPrice = (BigDecimal) item.get("unit_price");
        BigDecimal gross = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        if (gross.compareTo(new BigDecimal("9999999999999999")) > 0) {
            throw new BusinessException("Thành tiền vượt giới hạn dữ liệu.");
        }

        long productId = PosSupport.id(product, "id");
        var promoOpt = autoPromoAllowed ? db.activePromotionForProduct(productId, PosSupport.now()) : java.util.Optional.<Map<String, Object>>empty();
        BigDecimal discount = BigDecimal.ZERO;
        Long promoId = null;
        String promoCode = null;
        String promoName = null;

        if (promoOpt.isPresent()) {
            var promo = promoOpt.get();
            promoId = PosSupport.id(promo, "id");
            promoCode = (String) promo.get("code");
            promoName = (String) promo.get("name");
            BigDecimal percent = (BigDecimal) promo.get("discount_percent");
            discount = gross.multiply(percent).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).setScale(2);
        }
        BigDecimal lineTotal = gross.subtract(discount);
        db.updateItemPromotion(PosSupport.id(item, "id"), quantity, discount, lineTotal, promoId, promoCode, promoName);
    }

    @Transactional
    public void quantity(String username, long id, long itemId, String input, boolean confirmed) {
        var order = ownedOrder(username, id);
        editable(order);
        var items = db.items(id);
        var item = items.stream().filter(row -> PosSupport.id(row, "id") == itemId).findFirst()
                .orElseThrow(() -> new BusinessException("Dòng sản phẩm không thuộc đơn hàng."));
        var product = db.product(PosSupport.id(item, "product_id"), true).orElseThrow();
        BigDecimal quantity = PosSupport.quantity(input, ((Number) product.get("quantity_scale")).intValue(), "Số lượng");
        if (quantity.signum() == 0) {
            if (!confirmed) {
                throw new BusinessException("Giảm về 0 cần xác nhận xóa dòng sản phẩm.");
            }
            db.removeItem(itemId);
        } else {
            if (quantity.compareTo(BigDecimal.ONE) < 0) {
                throw new BusinessException("Số lượng nhỏ nhất là 1; chọn xóa dòng nếu không mua.");
            }
            updateQuantity(item, product, quantity, isAutoPromoAllowed(order, items));
        }
        recalculate(id);
    }

    private void recalculate(long order) {
        var items = db.items(order);
        BigDecimal subtotal = items.stream().map(row -> ((BigDecimal) row.get("quantity"))
                        .multiply((BigDecimal) row.get("unit_price")).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = items.stream().map(row -> (BigDecimal) row.get("discount_amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = items.stream().map(row -> (BigDecimal) row.get("line_total"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(new BigDecimal("9999999999999999")) > 0
                || total.compareTo(new BigDecimal("9999999999999999")) > 0 || total.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("Tổng tiền cần là số nguyên đồng trong giới hạn dữ liệu.");
        }
        db.totals(order, subtotal, discount, total);
    }

    @Transactional
    public void cancel(String username, long order, String reason, boolean confirmed) {
        editable(ownedOrder(username, order));
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận hủy đơn hàng.");
        }
        String text = reason == null ? "" : reason.strip();
        if (text.length() > 255) {
            throw new BusinessException("Lý do hủy tối đa 255 ký tự.");
        }
        db.cancel(order, text, PosSupport.now());
    }

    private String method(String value) {
        if (!"CASH".equals(value) && !"BANK_TRANSFER".equals(value)) {
            throw new BusinessException("Chỉ hỗ trợ tiền mặt hoặc chuyển khoản QR.");
        }
        return value;
    }

    private String reference(String value) {
        String result = PosSupport.text(value, "Mã tham chiếu ngân hàng", 100).toUpperCase(Locale.ROOT);
        if (!result.matches("[A-Z0-9][A-Z0-9._/-]{0,99}")) {
            throw new BusinessException("Mã tham chiếu chỉ gồm chữ, số và dấu . _ / -.");
        }
        return result;
    }

    @Transactional
    public long pay(String username, long orderId, Forms.Payment form) {
        var order = ownedOrder(username, orderId);
        long actor = PosSupport.id(PosSupport.actor(db, username, "CASHIER"), "id");
        String method = method(form.method());
        BigDecimal received = PosSupport.money(form.received(), "Số tiền nhận");
        String key = PosSupport.requestKey(form.requestKey());
        String reference = "BANK_TRANSFER".equals(method) ? reference(form.reference()) : null;
        if (reference != null && (!Boolean.TRUE.equals(form.verified()) || !transfer.configured())) {
            throw new BusinessException("Cần cấu hình tài khoản nhận tiền và xác nhận đã kiểm tra tiền vào tài khoản cửa hàng.");
        }
        var replay = db.paymentByKey(key);
        if (replay.isPresent()) {
            var old = replay.get();
            if (PosSupport.id(old, "order_id") != orderId || !method.equals(old.get("method"))
                    || !"SUCCESS".equals(old.get("status"))
                    || received.compareTo((BigDecimal) old.get("amount_received")) != 0
                    || !java.util.Objects.equals(reference, old.get("bank_reference"))) {
                throw new BusinessException("Mã yêu cầu đã được sử dụng cho lần thanh toán khác.");
            }
            return PosSupport.id(db.invoiceForOrder(orderId).orElseThrow(), "id");
        }
        editable(order);
        List<Map<String, Object>> items = db.items(orderId);
        if (items.isEmpty()) {
            throw new BusinessException("Đơn hàng chưa có sản phẩm.");
        }
        var now = PosSupport.now();
        String mode = currentDiscountMode(order, items);
        if ("COUPON".equals(mode)) {
            if (refreshOrderCoupon(order, items, now)) {
                order = ownedOrder(username, orderId);
                throw new BusinessException("Mã coupon đã hết hiệu lực. Đơn hàng đã được tính lại tổng mới ("
                        + order.get("total_amount") + " VNĐ). Vui lòng xác nhận trước khi thanh toán.");
            }
        } else if (!"DIRECT_ITEM".equals(mode) && !"DIRECT_ORDER".equals(mode)) {
            boolean priceChanged = refreshOrderPromotions(orderId, items, now);
            if (priceChanged) {
                order = ownedOrder(username, orderId);
                items = db.items(orderId);
                throw new BusinessException("Khuyến mãi đã thay đổi hiệu lực. Đơn hàng đã được tính lại tổng mới ("
                        + order.get("total_amount") + " VNĐ). Vui lòng xác nhận trước khi thanh toán.");
            }
        }
        checkTotals(order, items);
        BigDecimal due = (BigDecimal) order.get("total_amount");
        if (received.compareTo(due) < 0 || (reference != null && received.compareTo(due) != 0)) {
            throw new BusinessException(reference == null ? "Số tiền khách đưa chưa đủ." : "Tiền chuyển khoản phải đúng số phải trả, không thiếu hoặc thừa.");
        }
        if (reference != null && db.referenceExists(reference)) {
            throw new BusinessException("Mã tham chiếu đã được sử dụng cho giao dịch khác.");
        }
        for (var item : items) {
            var product = db.product(PosSupport.id(item, "product_id"), true).orElseThrow();
            if (!"ACTIVE".equals(product.get("status"))) {
                throw new BusinessException("Đơn có sản phẩm đã ngừng bán.");
            }
            BigDecimal quantity = (BigDecimal) item.get("quantity");
            available(product, quantity);
            db.deductStock(PosSupport.id(product, "id"), quantity, now);
            db.movement(item, now);
        }
        long payment = db.createPayment(orderId, method, "SUCCESS", due, received, received.subtract(due), key,
                reference, actor, now);
        db.markPaid(orderId, now);
        return db.createInvoice(orderId, payment, actor, PosSupport.number("HD-"), now);
    }

    @Transactional
    public void applyCoupon(String username, long orderId, String couponCode, boolean confirmed) {
        var order = ownedOrder(username, orderId);
        editable(order);
        var items = db.items(orderId);
        if (items.isEmpty()) {
            throw new BusinessException("Đơn hàng chưa có sản phẩm.");
        }
        String code = PosSupport.text(couponCode, "Mã coupon", 30).toUpperCase(Locale.ROOT);
        var coupon = db.couponByCode(code)
                .orElseThrow(() -> new BusinessException("Mã coupon không tồn tại."));
        var now = PosSupport.now();
        if (!"ENABLED".equals(coupon.get("lifecycle_status"))
                || now.before((java.sql.Timestamp) coupon.get("starts_at"))
                || !now.before((java.sql.Timestamp) coupon.get("ends_at"))) {
            throw new BusinessException("Mã coupon chưa đến hạn hoặc đã hết hiệu lực.");
        }
        BigDecimal subtotal = orderGrossSubtotal(items);
        BigDecimal minOrder = (BigDecimal) coupon.get("min_order_amount");
        if (minOrder != null && subtotal.compareTo(minOrder) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng coupon.");
        }
        String type = String.valueOf(coupon.get("discount_type"));
        BigDecimal value = (BigDecimal) coupon.get("discount_value");
        BigDecimal discount = "PERCENT".equals(type)
                ? subtotal.multiply(value).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).setScale(2)
                : value.setScale(0, RoundingMode.HALF_UP).setScale(2);
        if (discount.signum() <= 0 || discount.compareTo(subtotal) > 0) {
            throw new BusinessException("Giá trị giảm của coupon không hợp lệ hoặc vượt quá tổng đơn.");
        }

        String currentMode = currentDiscountMode(order, items);
        if (!"NONE".equals(currentMode) && !confirmed) {
            throw new BusinessException("Đơn hàng đang áp dụng ưu đãi khác. Vui lòng xác nhận thay thế (không cộng dồn).");
        }

        clearDiscounts(orderId, items);
        allocateOrderDiscount(items, discount);
        db.orderDiscount(orderId, "COUPON", PosSupport.id(coupon, "id"), code, null);
        recalculate(orderId);
    }

    @Transactional
    public void applyDirectDiscount(String username, long orderId, Forms.DirectDiscount form) {
        var order = ownedOrder(username, orderId);
        editable(order);
        var items = db.items(orderId);
        if (items.isEmpty()) {
            throw new BusinessException("Đơn hàng chưa có sản phẩm.");
        }

        String scope = form.scope() == null ? "ORDER" : form.scope().strip().toUpperCase(Locale.ROOT);
        String type = form.type() == null ? "PERCENT" : form.type().strip().toUpperCase(Locale.ROOT);
        if (!"ORDER".equals(scope) && !"ITEM".equals(scope)) {
            throw new BusinessException("Phạm vi giảm giá không hợp lệ.");
        }
        if (!"PERCENT".equals(type) && !"AMOUNT".equals(type)) {
            throw new BusinessException("Loại giảm giá không hợp lệ.");
        }
        BigDecimal rawValue = parsePositiveDecimal(form.value(), "Giá trị giảm");
        if ("PERCENT".equals(type) && rawValue.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Phần trăm giảm giá phải nằm trong khoảng lớn hơn 0 đến 100.");
        }

        BigDecimal orderSubtotal = orderGrossSubtotal(items);
        Map<String, Object> targetItem = null;
        BigDecimal targetGross = orderSubtotal;
        if ("ITEM".equals(scope)) {
            if (form.itemId() == null) {
                throw new BusinessException("Vui lòng chọn dòng sản phẩm cần giảm giá.");
            }
            targetItem = items.stream()
                    .filter(row -> PosSupport.id(row, "id") == form.itemId())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Dòng sản phẩm không thuộc đơn hàng."));
            targetGross = itemGross(targetItem);
        }

        BigDecimal totalDiscount = "PERCENT".equals(type)
                ? targetGross.multiply(rawValue).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).setScale(2)
                : rawValue.setScale(0, RoundingMode.HALF_UP).setScale(2);

        if (totalDiscount.signum() <= 0 || totalDiscount.compareTo(targetGross) > 0) {
            throw new BusinessException("Giá trị giảm không hợp lệ hoặc làm số tiền phải trả bị âm.");
        }

        String currentMode = currentDiscountMode(order, items);
        if (!"NONE".equals(currentMode) && !form.confirmed()) {
            throw new BusinessException("Đơn hàng đang áp dụng ưu đãi khác. Vui lòng xác nhận thay thế (không cộng dồn).");
        }

        BigDecimal cashierLimit = orderSubtotal.multiply(CASHIER_DISCOUNT_LIMIT_RATE)
                .setScale(0, RoundingMode.HALF_UP).setScale(2);
        Long approvedBy = null;
        if (totalDiscount.compareTo(cashierLimit) > 0) {
            approvedBy = verifyManagerApproval(form.managerUsername(), form.managerPassword());
        }

        clearDiscounts(orderId, items);
        if ("ORDER".equals(scope)) {
            allocateOrderDiscount(items, totalDiscount);
            db.orderDiscount(orderId, "DIRECT_ORDER", null, null, approvedBy);
        } else {
            BigDecimal lineTotal = targetGross.subtract(totalDiscount);
            db.updateItemPromotion(PosSupport.id(targetItem, "id"), (BigDecimal) targetItem.get("quantity"),
                    totalDiscount, lineTotal, null, null, null);
            db.orderDiscount(orderId, "DIRECT_ITEM", null, null, approvedBy);
        }
        recalculate(orderId);
    }

    @Transactional
    public void clearOrderDiscount(String username, long orderId) {
        var order = ownedOrder(username, orderId);
        editable(order);
        var items = db.items(orderId);
        clearDiscounts(orderId, items);
        recalculate(orderId);
    }

    private Long verifyManagerApproval(String managerUsername, String managerPassword) {
        String username = managerUsername == null ? "" : managerUsername.strip();
        String password = managerPassword == null ? "" : managerPassword;
        if (username.isEmpty() || password.isEmpty()) {
            throw new BusinessException("Khoản giảm vượt hạn mức 10% của Thu ngân. Cần Quản lý ca phê duyệt.");
        }
        var manager = db.account(username, true)
                .filter(acc -> "ACTIVE".equals(acc.get("status")) && "MANAGER".equals(acc.get("role_code")))
                .orElseThrow(() -> new BusinessException("Tài khoản phê duyệt không hợp lệ hoặc không có quyền Quản lý."));
        String hash = (String) manager.get("password_hash");
        if (hash == null || !passwordEncoder.matches(password, hash)) {
            throw new BusinessException("Mật khẩu phê duyệt của Quản lý ca không chính xác.");
        }
        return PosSupport.id(manager, "id");
    }

    private BigDecimal parsePositiveDecimal(String input, String label) {
        try {
            BigDecimal val = new BigDecimal(input == null ? "" : input.strip());
            if (val.signum() <= 0 || val.compareTo(new BigDecimal("9999999999999999")) > 0) {
                throw new NumberFormatException();
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new BusinessException(label + " phải là số dương hợp lệ.");
        }
    }

    private BigDecimal itemGross(Map<String, Object> item) {
        return ((BigDecimal) item.get("quantity")).multiply((BigDecimal) item.get("unit_price"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal orderGrossSubtotal(List<Map<String, Object>> items) {
        return items.stream().map(this::itemGross).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isAutoPromoAllowed(Map<String, Object> order, List<Map<String, Object>> items) {
        String mode = currentDiscountMode(order, items);
        return "NONE".equals(mode) || "AUTO_PROMO".equals(mode);
    }

    private String currentDiscountMode(Map<String, Object> order, List<Map<String, Object>> items) {
        Object rawMode = order.get("discount_mode");
        if (rawMode != null && !"NONE".equals(rawMode.toString())) {
            return rawMode.toString();
        }
        boolean hasPromo = items.stream().anyMatch(row ->
                row.get("promotion_id") != null || ((BigDecimal) row.get("discount_amount")).signum() != 0);
        return hasPromo ? "AUTO_PROMO" : "NONE";
    }

    private void clearDiscounts(long orderId, List<Map<String, Object>> items) {
        for (var item : items) {
            BigDecimal gross = itemGross(item);
            db.updateItemPromotion(PosSupport.id(item, "id"), (BigDecimal) item.get("quantity"),
                    BigDecimal.ZERO, gross, null, null, null);
        }
        db.orderDiscount(orderId, "NONE", null, null, null);
    }

    private void allocateOrderDiscount(List<Map<String, Object>> items, BigDecimal totalDiscount) {
        BigDecimal orderGross = orderGrossSubtotal(items);
        BigDecimal allocatedSum = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            BigDecimal gross = itemGross(item);
            BigDecimal itemDiscount;
            if (i < items.size() - 1) {
                itemDiscount = totalDiscount.multiply(gross).divide(orderGross, 0, RoundingMode.HALF_UP).setScale(2);
                if (itemDiscount.compareTo(gross) > 0) {
                    itemDiscount = gross;
                }
                allocatedSum = allocatedSum.add(itemDiscount);
            } else {
                itemDiscount = totalDiscount.subtract(allocatedSum).setScale(2, RoundingMode.HALF_UP);
                if (itemDiscount.signum() < 0) {
                    itemDiscount = BigDecimal.ZERO;
                }
                if (itemDiscount.compareTo(gross) > 0) {
                    itemDiscount = gross;
                }
            }
            BigDecimal lineTotal = gross.subtract(itemDiscount);
            db.updateItemPromotion(PosSupport.id(item, "id"), (BigDecimal) item.get("quantity"),
                    itemDiscount, lineTotal, null, null, null);
        }
    }

    private boolean refreshOrderCoupon(Map<String, Object> order, List<Map<String, Object>> items, java.sql.Timestamp now) {
        String code = (String) order.get("coupon_code_snapshot");
        var couponOpt = code == null ? java.util.Optional.<Map<String, Object>>empty() : db.couponByCode(code);
        BigDecimal subtotal = orderGrossSubtotal(items);
        if (couponOpt.isEmpty()) {
            clearDiscounts(PosSupport.id(order, "id"), items);
            recalculate(PosSupport.id(order, "id"));
            return true;
        }
        var coupon = couponOpt.get();
        BigDecimal minOrder = (BigDecimal) coupon.get("min_order_amount");
        if (!"ENABLED".equals(coupon.get("lifecycle_status"))
                || now.before((java.sql.Timestamp) coupon.get("starts_at"))
                || !now.before((java.sql.Timestamp) coupon.get("ends_at"))
                || (minOrder != null && subtotal.compareTo(minOrder) < 0)) {
            clearDiscounts(PosSupport.id(order, "id"), items);
            recalculate(PosSupport.id(order, "id"));
            return true;
        }
        return false;
    }

    private boolean refreshOrderPromotions(long orderId, List<Map<String, Object>> items, Object now) {
        boolean changed = false;
        for (var item : items) {
            long productId = PosSupport.id(item, "product_id");
            var promoOpt = db.activePromotionForProduct(productId, now);
            BigDecimal quantity = (BigDecimal) item.get("quantity");
            BigDecimal unitPrice = (BigDecimal) item.get("unit_price");
            BigDecimal gross = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            BigDecimal newDiscount = BigDecimal.ZERO;
            Long newPromoId = null;
            String newPromoCode = null;
            String newPromoName = null;

            if (promoOpt.isPresent()) {
                var promo = promoOpt.get();
                newPromoId = PosSupport.id(promo, "id");
                newPromoCode = (String) promo.get("code");
                newPromoName = (String) promo.get("name");
                BigDecimal percent = (BigDecimal) promo.get("discount_percent");
                newDiscount = gross.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            BigDecimal newLineTotal = gross.subtract(newDiscount);

            Long oldPromoId = item.get("promotion_id") == null ? null : PosSupport.id(item, "promotion_id");
            BigDecimal oldDiscount = (BigDecimal) item.get("discount_amount");

            if (!java.util.Objects.equals(oldPromoId, newPromoId) || oldDiscount.compareTo(newDiscount) != 0) {
                db.updateItemPromotion(PosSupport.id(item, "id"), quantity, newDiscount, newLineTotal,
                        newPromoId, newPromoCode, newPromoName);
                changed = true;
            }
        }
        if (changed) {
            recalculate(orderId);
        }
        return changed;
    }

    private void checkTotals(Map<String, Object> order, List<Map<String, Object>> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (var item : items) {
            BigDecimal gross = ((BigDecimal) item.get("quantity")).multiply((BigDecimal) item.get("unit_price"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal reduction = (BigDecimal) item.get("discount_amount");
            BigDecimal line = (BigDecimal) item.get("line_total");
            if (gross.signum() <= 0 || reduction.signum() < 0 || reduction.compareTo(gross) > 0
                    || line.compareTo(gross.subtract(reduction)) != 0) {
                throw new BusinessException("Dữ liệu dòng hàng không hợp lệ, chưa thể thanh toán.");
            }
            subtotal = subtotal.add(gross);
            discount = discount.add(reduction);
            total = total.add(line);
        }
        if (subtotal.compareTo((BigDecimal) order.get("subtotal_amount")) != 0
                || discount.compareTo((BigDecimal) order.get("discount_amount")) != 0
                || total.compareTo((BigDecimal) order.get("total_amount")) != 0
                || total.compareTo(subtotal.subtract(discount)) != 0) {
            throw new BusinessException("Tổng đơn hàng không khớp các dòng, chưa thể thanh toán.");
        }
    }

    @Transactional
    public void fail(String username, long orderId, String method, String requestKey) {
        var order = ownedOrder(username, orderId);
        String key = PosSupport.requestKey(requestKey);
        method(method);
        var replay = db.paymentByKey(key);
        if (replay.isPresent()) {
            var old = replay.get();
            if (PosSupport.id(old, "order_id") == orderId && "FAILED".equals(old.get("status")) && method.equals(old.get("method"))) {
                return;
            }
            throw new BusinessException("Mã yêu cầu đã được sử dụng.");
        }
        editable(order);
        db.createPayment(orderId, method, "FAILED", order.get("total_amount"), BigDecimal.ZERO, BigDecimal.ZERO,
                key, null, null, PosSupport.now());
    }

    @Transactional
    public Map<String, Object> invoice(String username, long id) {
        var actor = PosSupport.actor(db, username, null);
        var invoice = db.invoice(id).orElseThrow(() -> new BusinessException("Hóa đơn không tồn tại."));
        if (!"MANAGER".equals(actor.get("role_code")) && PosSupport.id(invoice, "issued_by") != PosSupport.id(actor, "id")) {
            throw new AccessDeniedException("Không có quyền xem hóa đơn này.");
        }
        invoice.put("items", db.items(PosSupport.id(invoice, "order_id")));
        return invoice;
    }
}
