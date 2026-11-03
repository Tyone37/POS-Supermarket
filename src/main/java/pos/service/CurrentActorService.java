package pos.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.CurrentActor;
import pos.entity.Account;
import pos.entity.AccountStatus;
import pos.entity.Employee;
import pos.entity.ReferenceStatus;
import pos.repository.AccountRepository;
import pos.repository.EmployeeRepository;

@Service
public class CurrentActorService {
    private final AccountRepository accounts;
    private final EmployeeRepository employees;

    public CurrentActorService(AccountRepository accounts, EmployeeRepository employees) {
        this.accounts = accounts;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public CurrentActor requireEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Không có quyền truy cập.");
        }
        Account account = accounts.findByUsername(authentication.getName())
                .filter(value -> value.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền truy cập."));
        Employee employee = employees.findByAccountId(account.getId())
                .filter(value -> value.getStatus() == ReferenceStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền truy cập."));
        return new CurrentActor(account.getId(), employee.getId(), account.getUsername(), account.getFullName());
    }
}
