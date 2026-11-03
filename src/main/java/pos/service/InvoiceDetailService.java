package pos.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pos.dto.CurrentActor;
import pos.dto.InvoiceDetail;
import pos.dto.InvoiceLineView;
import pos.entity.Invoice;
import pos.entity.Payment;
import pos.repository.InvoiceRepository;
import pos.repository.OrderItemRepository;

@Service
@PreAuthorize("hasAuthority('INVOICE_VIEW')")
public class InvoiceDetailService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final CurrentActorService actors;
    private final InvoiceRepository invoices;
    private final OrderItemRepository items;

    public InvoiceDetailService(CurrentActorService actors, InvoiceRepository invoices, OrderItemRepository items) {
        this.actors = actors;
        this.invoices = invoices;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public InvoiceDetail getDetail(Long id) {
        CurrentActor actor = actors.requireEmployee();
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return detail(invoice, actor);
    }

    @Transactional(readOnly = true)
    public InvoiceDetail findByNumber(String number) {
        CurrentActor actor = actors.requireEmployee();
        String cleaned = number == null ? "" : number.strip();
        if (cleaned.isEmpty() || cleaned.length() > 30) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Invoice invoice = invoices.findByInvoiceNo(cleaned)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return detail(invoice, actor);
    }

    private InvoiceDetail detail(Invoice invoice, CurrentActor actor) {
        if (!invoice.getIssuedBy().getId().equals(actor.accountId())
                || !invoice.getOrder().getShift().getEmployee().getId().equals(actor.employeeId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<InvoiceLineView> lines = items.findByOrderIdOrderByIdAsc(invoice.getOrder().getId()).stream()
                .map(item -> new InvoiceLineView(item.getProductCodeSnapshot(), item.getProductNameSnapshot(),
                        item.getBarcodeSnapshot(), item.getUnitCodeSnapshot(), item.getQuantity(), item.getUnitPrice(),
                        item.getPromotionCodeSnapshot(), item.getPromotionNameSnapshot(), item.getDiscountAmount(),
                        item.getLineTotal())).toList();
        Payment payment = invoice.getPayment();
        return new InvoiceDetail(invoice.getId(), invoice.getInvoiceNo(), DISPLAY_TIME.format(invoice.getIssuedAt()),
                invoice.getIssuedBy().getFullName(), invoice.getOrder().getShift().getRegister().getName(),
                invoice.getStatus().name(), payment.getMethod(), payment.getStatus().name(),
                invoice.getSubtotalAmount(), invoice.getDiscountAmount(), invoice.getTotalAmount(),
                payment.getAmountDue(), payment.getAmountReceived(), payment.getChangeAmount(), lines);
    }
}
