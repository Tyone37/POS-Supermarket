package pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pos.dto.CurrentActor;
import pos.dto.PaymentScope;
import pos.entity.Invoice;
import pos.entity.InvoiceStatus;
import pos.entity.Order;
import pos.entity.OrderItem;
import pos.entity.OrderStatus;
import pos.entity.Payment;
import pos.entity.PaymentStatus;
import pos.entity.Shift;
import pos.entity.ShiftStatus;
import pos.exception.FormValidationException;
import pos.repository.AccountRepository;
import pos.repository.InvoiceRepository;
import pos.repository.OrderItemRepository;
import pos.repository.OrderRepository;
import pos.repository.PaymentRepository;
import pos.repository.ShiftRepository;

@Service
@PreAuthorize("hasAuthority('INVOICE_ISSUE')")
public class InvoiceService {

    private final CurrentActorService actors;
    private final ShiftRepository shifts;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final OrderItemRepository items;
    private final AccountRepository accounts;

    public InvoiceService(CurrentActorService actors, ShiftRepository shifts, OrderRepository orders,
            PaymentRepository payments, InvoiceRepository invoices, OrderItemRepository items,
            AccountRepository accounts) {
        this.actors = actors;
        this.shifts = shifts;
        this.orders = orders;
        this.payments = payments;
        this.invoices = invoices;
        this.items = items;
        this.accounts = accounts;
    }

    /** Called by checkout after marking SUCCESS/PAID, inside its existing transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long issue(Long paymentId) {
        CurrentActor actor = actors.requireEmployee();
        PaymentScope scope = payments.findScopeById(paymentId).orElseThrow(this::notFound);
        Shift shift = shifts.findLockedById(scope.shiftId())
                .filter(value -> value.getEmployee().getId().equals(actor.employeeId()))
                .orElseThrow(this::notFound);
        Order order = orders.findLockedById(scope.orderId()).orElseThrow(this::notFound);
        Payment payment = payments.findLockedById(paymentId).orElseThrow(this::notFound);
        if (!order.getShift().getId().equals(shift.getId()) || !payment.getOrder().getId().equals(order.getId())
                || order.getStatus() != OrderStatus.PAID || order.getPaidAt() == null
                || payment.getStatus() != PaymentStatus.SUCCESS || payment.getCompletedAt() == null
                || !"CASH".equals(payment.getMethod()) || payment.getAmountDue().compareTo(order.getTotalAmount()) != 0
                || payment.getAmountReceived().compareTo(payment.getAmountDue()) < 0
                || payment.getChangeAmount().compareTo(payment.getAmountReceived().subtract(payment.getAmountDue())) != 0) {
            throw invalidPayment();
        }
        checkSnapshots(order);
        Invoice existing = invoices.findByOrderId(order.getId())
                .or(() -> invoices.findByPaymentId(paymentId)).orElse(null);
        if (existing != null) {
            if (!existing.getOrder().getId().equals(order.getId())
                    || !existing.getPayment().getId().equals(paymentId)
                    || existing.getStatus() != InvoiceStatus.ISSUED
                    || existing.getSubtotalAmount().compareTo(order.getSubtotalAmount()) != 0
                    || existing.getDiscountAmount().compareTo(order.getDiscountAmount()) != 0
                    || existing.getTotalAmount().compareTo(order.getTotalAmount()) != 0) {
                throw invalidPayment();
            }
            return existing.getId();
        }
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new FormValidationException(Map.of("", "Ca đã đóng, không thể phát hành hóa đơn mới"));
        }
        String number = "HD" + UUID.randomUUID().toString().replace("-", "").substring(0, 28)
                .toUpperCase(Locale.ROOT);
        Invoice invoice = new Invoice(number, order, payment,
                accounts.findById(actor.accountId()).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ")),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return invoices.saveAndFlush(invoice).getId();
    }

    private void checkSnapshots(Order order) {
        List<OrderItem> lines = items.findByOrderIdOrderByIdAsc(order.getId());
        if (lines.isEmpty()) {
            throw invalidPayment();
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem line : lines) {
            BigDecimal gross = line.getQuantity().multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            if (line.getQuantity().signum() <= 0 || line.getUnitPrice().signum() <= 0
                    || line.getDiscountAmount().signum() < 0 || line.getDiscountAmount().compareTo(gross) > 0
                    || line.getLineTotal().compareTo(gross.subtract(line.getDiscountAmount())) != 0) {
                throw invalidPayment();
            }
            subtotal = subtotal.add(gross);
            discount = discount.add(line.getDiscountAmount());
            total = total.add(line.getLineTotal());
        }
        if (subtotal.compareTo(order.getSubtotalAmount()) != 0 || discount.compareTo(order.getDiscountAmount()) != 0
                || total.compareTo(order.getTotalAmount()) != 0 || total.compareTo(subtotal.subtract(discount)) != 0) {
            throw invalidPayment();
        }
    }

    private FormValidationException invalidPayment() {
        return new FormValidationException(Map.of("", "Dữ liệu đơn hàng và thanh toán chưa đủ điều kiện phát hành hóa đơn"));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thanh toán trong phạm vi được phép");
    }
}
