package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.InvoicePrint;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.PosRepository;

@Service
@Transactional(readOnly = true)
public class InvoicePrintService {
    private static final Set<String> READER_ROLES = Set.of("CASHIER", "MANAGER", "ADMIN");
    private final AccountRepository accounts;
    private final PosRepository invoices;

    public InvoicePrintService(AccountRepository accounts, PosRepository invoices) {
        this.accounts = accounts;
        this.invoices = invoices;
    }

    public InvoicePrint preview(String username, long id) {
        var account = accounts.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền xem hóa đơn."));
        if (!"ACTIVE".equals(account.status()) || !READER_ROLES.contains(account.roleCode())) {
            throw new AccessDeniedException("Không có quyền xem hóa đơn.");
        }
        if (id <= 0) {
            throw new BusinessException("Mã hóa đơn không hợp lệ.");
        }
        var invoice = invoices.invoice(id).orElseThrow(() -> new BusinessException("Hóa đơn không tồn tại."));
        if ("CASHIER".equals(account.roleCode()) && PosSupport.id(invoice, "issued_by") != account.id()) {
            throw new AccessDeniedException("Không có quyền xem hóa đơn này.");
        }
        var lines = invoices.invoiceItems(PosSupport.id(invoice, "order_id")).stream()
                .map(item -> new InvoicePrint.Line((String) item.get("product_code_snapshot"),
                        (String) item.get("product_name_snapshot"), (String) item.get("unit_code_snapshot"),
                        money(item, "quantity"), money(item, "unit_price"), money(item, "discount_amount"),
                        money(item, "line_total"))).toList();
        return new InvoicePrint(id, (String) invoice.get("invoice_no"), (Timestamp) invoice.get("issued_at"),
                (String) invoice.get("cashier_name"), (String) invoice.get("method"), (String) invoice.get("status"),
                (String) invoice.get("bank_reference"), money(invoice, "subtotal_amount"),
                money(invoice, "discount_amount"), money(invoice, "total_amount"),
                money(invoice, "amount_received"), money(invoice, "change_amount"), lines);
    }

    private BigDecimal money(Map<String, Object> values, String key) {
        return (BigDecimal) values.get(key);
    }
}
