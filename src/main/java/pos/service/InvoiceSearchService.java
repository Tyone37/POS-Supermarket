package pos.service;

import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.InvoiceSearch;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.InvoiceSearchRepository;

@Service
@Transactional(readOnly = true)
public class InvoiceSearchService {
    public static final int PAGE_SIZE = 20;
    private static final Set<String> READER_ROLES = Set.of("CASHIER", "MANAGER", "ADMIN");
    private static final Set<String> METHODS = Set.of("", "CASH", "BANK_TRANSFER");
    private static final Set<String> STATUSES = Set.of("", "ISSUED", "VOID");
    private final AccountRepository accounts;
    private final InvoiceSearchRepository invoices;

    public InvoiceSearchService(AccountRepository accounts, InvoiceSearchRepository invoices) {
        this.accounts = accounts;
        this.invoices = invoices;
    }

    public InvoiceSearch.Result search(String username, InvoiceSearch.Filter filter, int page) {
        var account = accounts.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền xem hóa đơn."));
        if (!"ACTIVE".equals(account.status()) || !READER_ROLES.contains(account.roleCode())) {
            throw new AccessDeniedException("Không có quyền xem hóa đơn.");
        }
        String code = value(filter.getCode()).strip();
        String method = value(filter.getMethod());
        String status = value(filter.getStatus());
        if (code.length() > 30 || !METHODS.contains(method) || !STATUSES.contains(status)) {
            throw new BusinessException("Bộ lọc hóa đơn không hợp lệ.");
        }
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new BusinessException("Trang kết quả không hợp lệ.");
        }
        Long issuer = "CASHIER".equals(account.roleCode()) ? account.id() : null;
        var query = new InvoiceSearch.Query(code, method, status, issuer, page * PAGE_SIZE);
        var rows = invoices.search(query, PAGE_SIZE + 1);
        return new InvoiceSearch.Result(rows.stream().limit(PAGE_SIZE).toList(), page, rows.size() > PAGE_SIZE);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
