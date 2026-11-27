package pos.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.ShiftHistory;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.repository.ShiftHistoryRepository;

@Service
@Transactional(readOnly = true)
public class ShiftHistoryService {
    public static final int PAGE_SIZE = 20;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AccountRepository accounts;
    private final ShiftHistoryRepository history;

    public ShiftHistoryService(AccountRepository accounts, ShiftHistoryRepository history) {
        this.accounts = accounts;
        this.history = history;
    }

    public ShiftHistory.SearchResult search(String username, ShiftHistory.Filter filter, int page) {
        authorize(username);
        validate(filter);
        int offset = offset(page);
        var query = new ShiftHistory.Query(start(filter.getFromDate()),
                filter.getToDate() == null ? null : start(filter.getToDate().plusDays(1)),
                filter.getCashierId(), filter.getRegisterId(), filter.getStatus() == null ? "" : filter.getStatus(), offset);
        return new ShiftHistory.SearchResult(page(history.search(query, PAGE_SIZE + 1), page), loadOptions());
    }

    public ShiftHistory.Options options(String username) {
        authorize(username);
        return loadOptions();
    }

    public ShiftHistory.DetailResult detail(String username, long id, int page) {
        authorize(username);
        int offset = offset(page);
        if (id <= 0) {
            throw new BusinessException("Mã ca không hợp lệ.");
        }
        var detail = history.detail(id).orElseThrow(() -> new BusinessException("Ca không tồn tại."));
        return new ShiftHistory.DetailResult(detail, page(history.invoices(id, offset, PAGE_SIZE + 1), page));
    }

    private void authorize(String username) {
        var account = accounts.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền xem lịch sử ca."));
        if (!"ACTIVE".equals(account.status())
                || (!"MANAGER".equals(account.roleCode()) && !"ADMIN".equals(account.roleCode()))) {
            throw new AccessDeniedException("Chỉ quản lý hoặc quản trị viên được xem toàn bộ ca.");
        }
    }

    private ShiftHistory.Options loadOptions() {
        return new ShiftHistory.Options(history.cashiers(), history.registers());
    }

    private Timestamp start(LocalDate date) {
        return date == null ? null : Timestamp.from(date.atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    private int offset(int page) {
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new BusinessException("Trang kết quả không hợp lệ.");
        }
        return page * PAGE_SIZE;
    }

    private void validate(ShiftHistory.Filter filter) {
        if (filter.getFromDate() != null && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        for (LocalDate date : new LocalDate[] {filter.getFromDate(), filter.getToDate()}) {
            if (date != null && (date.getYear() < 1000 || date.getYear() > 9998)) {
                throw new BusinessException("Ngày tìm kiếm phải nằm trong khoảng năm 1000 đến 9998.");
            }
        }
        if ((filter.getCashierId() != null && filter.getCashierId() <= 0)
                || (filter.getRegisterId() != null && filter.getRegisterId() <= 0)) {
            throw new BusinessException("Thu ngân hoặc quầy không hợp lệ.");
        }
        if (filter.getStatus() != null && !List.of("", "OPEN", "CLOSED").contains(filter.getStatus())) {
            throw new BusinessException("Trạng thái ca không hợp lệ.");
        }
    }

    private <T> ShiftHistory.Page<T> page(List<T> items, int number) {
        return new ShiftHistory.Page<>(items.stream().limit(PAGE_SIZE).toList(), number, items.size() > PAGE_SIZE);
    }
}
