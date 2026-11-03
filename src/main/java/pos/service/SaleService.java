package pos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Validator;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pos.dto.AddItemRequest;
import pos.dto.CurrentActor;
import pos.dto.SaleLineView;
import pos.dto.SaleView;
import pos.entity.Order;
import pos.entity.OrderItem;
import pos.entity.OrderStatus;
import pos.entity.Product;
import pos.entity.ProductStatus;
import pos.entity.Shift;
import pos.entity.ShiftStatus;
import pos.exception.FormValidationException;
import pos.repository.OrderItemRepository;
import pos.repository.OrderRepository;
import pos.repository.ProductRepository;
import pos.repository.ShiftRepository;

@Service
@PreAuthorize("hasAuthority('SALE_USE')")
public class SaleService {
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999999.999");
    private final CurrentActorService actors;
    private final ShiftRepository shifts;
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final Validator validator;

    public SaleService(CurrentActorService actors, ShiftRepository shifts, OrderRepository orders,
            OrderItemRepository items, ProductRepository products, Validator validator) {
        this.actors = actors;
        this.shifts = shifts;
        this.orders = orders;
        this.items = items;
        this.products = products;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public SaleView current() {
        CurrentActor actor = actors.requireEmployee();
        Shift shift = currentShift(actor);
        return orders.findFirstByShiftIdAndStatusOrderByIdDesc(shift.getId(), OrderStatus.DRAFT)
                .map(order -> toView(order, actor)).orElse(null);
    }

    @Transactional(readOnly = true)
    public SaleView view(Long id) {
        CurrentActor actor = actors.requireEmployee();
        Shift shift = currentShift(actor);
        Order order = orders.findById(id).filter(value -> value.getStatus() == OrderStatus.DRAFT
                && value.getShift().getId().equals(shift.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toView(order, actor);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long create() {
        CurrentActor actor = actors.requireEmployee();
        Shift shift = lockedShift(actor);
        Order existing = orders.findFirstByShiftIdAndStatusOrderByIdDesc(shift.getId(), OrderStatus.DRAFT)
                .orElse(null);
        if (existing != null) {
            return existing.getId();
        }
        String orderNo = "DH" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 28).toUpperCase(Locale.ROOT);
        Order order = new Order(orderNo, shift, Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return orders.saveAndFlush(order).getId();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addCode(Long id, AddItemRequest request) {
        if (request == null) {
            throw new FormValidationException(Map.of("", "Yêu cầu không hợp lệ."));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        validator.validate(request).forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        if (!errors.isEmpty()) {
            throw new FormValidationException(errors);
        }
        CurrentActor actor = actors.requireEmployee();
        Shift shift = lockedShift(actor);
        Order order = orders.findLockedById(id).filter(value -> value.getStatus() == OrderStatus.DRAFT
                && value.getShift().getId().equals(shift.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Long> productIds = products.findIdsByBarcodeOrCode(request.code());
        if (productIds.isEmpty()) {
            throw new FormValidationException(Map.of("code", "Không tìm thấy sản phẩm."));
        }
        if (productIds.size() > 1) {
            throw new FormValidationException(Map.of("code", "Mã khớp nhiều sản phẩm."));
        }
        Product product = products.findLockedById(productIds.get(0))
                .filter(value -> value.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new FormValidationException(Map.of("code", "Sản phẩm không khả dụng.")));
        OrderItem item = items.findByOrderIdAndProductId(order.getId(), product.getId()).orElse(null);
        if (item != null && (item.getPromotionId() != null || item.getDiscountAmount().signum() != 0)) {
            throw new FormValidationException(Map.of("code", "Không thể đổi lượng dòng có khuyến mãi."));
        }
        BigDecimal quantity = item == null ? BigDecimal.ONE : item.getQuantity().add(BigDecimal.ONE);
        if (quantity.compareTo(BigDecimal.ONE) < 0 || quantity.compareTo(MAX_QUANTITY) > 0
                || quantity.stripTrailingZeros().scale() > product.getUnit().getQuantityScale()) {
            throw new FormValidationException(Map.of("code", "Số lượng không hợp lệ."));
        }
        if (quantity.compareTo(product.getStockQuantity().subtract(product.getReservedQuantity())) > 0) {
            throw new FormValidationException(Map.of("code", "Số lượng vượt tồn khả dụng."));
        }
        if (item == null) {
            item = new OrderItem(order, product, quantity);
        } else {
            item.changeQuantity(quantity);
        }
        if (item.getLineTotal().compareTo(new BigDecimal("9999999999999999.99")) > 0) {
            throw new FormValidationException(Map.of("code", "Tổng tiền vượt giới hạn cho phép."));
        }
        items.saveAndFlush(item);
        order.recalculate(items.findByOrderIdOrderByIdAsc(order.getId()));
        if (order.getSubtotalAmount().compareTo(new BigDecimal("9999999999999999.99")) > 0
                || order.getTotalAmount().compareTo(new BigDecimal("9999999999999999.99")) > 0) {
            throw new FormValidationException(Map.of("code", "Tổng tiền vượt giới hạn cho phép."));
        }
        orders.saveAndFlush(order);
    }

    private Shift currentShift(CurrentActor actor) {
        return shifts.findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(actor.employeeId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new FormValidationException(Map.of("", "Vui lòng mở ca trước khi bán hàng.")));
    }

    private Shift lockedShift(CurrentActor actor) {
        return shifts.findOpenLocked(actor.employeeId())
                .orElseThrow(() -> new FormValidationException(Map.of("", "Vui lòng mở ca trước khi bán hàng.")));
    }

    private SaleView toView(Order order, CurrentActor actor) {
        List<SaleLineView> lines = items.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(item -> new SaleLineView(item.getId(), item.getProduct().getId(),
                        item.getProductCodeSnapshot(), item.getProductNameSnapshot(), item.getBarcodeSnapshot(),
                        item.getUnitCodeSnapshot(), item.getQuantity(), item.getUnitPrice(),
                        item.getDiscountAmount(), item.getLineTotal())).toList();
        return new SaleView(order.getId(), order.getOrderNo(), order.getStatus().name(),
                order.getShift().getId(), order.getShift().getRegister().getName(), actor.fullName(), lines,
                order.getSubtotalAmount(), order.getDiscountAmount(), order.getTotalAmount());
    }
}
