package pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class SaleService {
    private final PosRepository db;
    private final TransferSettings transfer;
    public SaleService(PosRepository db, TransferSettings transfer) { this.db = db; this.transfer = transfer; }

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
        var found = db.productByCode(PosSupport.text(code, "Mã sản phẩm", 50))
                .orElseThrow(() -> new BusinessException("Không tìm thấy mã sản phẩm đang bán."));
        var product = db.product(PosSupport.id(found, "id"), true).orElseThrow();
        if (!"ACTIVE".equals(product.get("status"))) {
            throw new BusinessException("Sản phẩm đã ngừng bán.");
        }
        var existing = db.items(id).stream().filter(row -> PosSupport.id(row, "product_id") == PosSupport.id(product, "id")).findFirst();
        if (existing.isPresent()) {
            var item = existing.get();
            updateQuantity(item, product, ((BigDecimal) item.get("quantity")).add(BigDecimal.ONE));
        } else {
            available(product, BigDecimal.ONE);
            db.createItem(id, product);
        }
        recalculate(id);
    }

    private void available(Map<String, Object> product, BigDecimal quantity) {
        BigDecimal stock = ((BigDecimal) product.get("stock_quantity")).subtract((BigDecimal) product.get("reserved_quantity"));
        if (stock.compareTo(quantity) < 0) {
            throw new BusinessException("Tồn kho khả dụng không đủ cho sản phẩm " + product.get("name") + ".");
        }
    }

    private void updateQuantity(Map<String, Object> item, Map<String, Object> product, BigDecimal quantity) {
        available(product, quantity);
        BigDecimal total = quantity.multiply((BigDecimal) item.get("unit_price")).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(new BigDecimal("9999999999999999")) > 0) {
            throw new BusinessException("Thành tiền vượt giới hạn dữ liệu.");
        }
        db.quantity(PosSupport.id(item, "id"), quantity, total);
    }

    @Transactional
    public void quantity(String username, long id, long itemId, String input, boolean confirmed) {
        editable(ownedOrder(username, id));
        var item = db.items(id).stream().filter(row -> PosSupport.id(row, "id") == itemId).findFirst()
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
            updateQuantity(item, product, quantity);
        }
        recalculate(id);
    }

    private void recalculate(long order) {
        BigDecimal total = db.items(order).stream().map(row -> (BigDecimal) row.get("line_total"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("9999999999999999")) > 0 || total.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("Tổng tiền cần là số nguyên đồng trong giới hạn dữ liệu.");
        }
        db.totals(order, total);
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
        BigDecimal due = (BigDecimal) order.get("total_amount");
        if (received.compareTo(due) < 0 || (reference != null && received.compareTo(due) != 0)) {
            throw new BusinessException(reference == null ? "Số tiền khách đưa chưa đủ." : "Tiền chuyển khoản phải đúng số phải trả, không thiếu hoặc thừa.");
        }
        if (reference != null && db.referenceExists(reference)) {
            throw new BusinessException("Mã tham chiếu đã được sử dụng cho giao dịch khác.");
        }
        var now = PosSupport.now();
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
