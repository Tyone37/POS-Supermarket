package pos.repository;

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pos.entity.Account;
import pos.entity.AccountEntity;

@Repository
public class AccountRepository {
    private final EntityManager entityManager;

    public AccountRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByUsername(String username) {
        var matches = entityManager.createQuery("""
                select a, r.code from AccountEntity a, RoleEntity r
                where a.roleId = r.id and a.username = :username
                """, Object[].class)
                .setParameter("username", username)
                .getResultList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            throw new jakarta.persistence.NonUniqueResultException();
        }
        var row = matches.get(0);
        var account = (AccountEntity) row[0];
        var permissions = entityManager.createQuery("""
                select p.code from RolePermissionEntity rp, PermissionEntity p
                where rp.permissionId = p.id and rp.roleId = :roleId
                """, String.class)
                .setParameter("roleId", account.getRoleId())
                .getResultList();
        return Optional.of(new Account(account.getId(), account.getUsername(), account.getPasswordHash(),
                account.getFullName(), (String) row[1], account.getStatus(), Set.copyOf(permissions)));
    }
}
