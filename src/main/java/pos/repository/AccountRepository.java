package pos.repository;

import java.util.Set;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pos.entity.Account;

@Repository
public class AccountRepository {
    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findByUsername(String username) {
        return jdbc.sql("""
                SELECT a.id, a.username, a.password_hash, a.full_name, a.status, r.code AS role_code
                FROM accounts a JOIN roles r ON r.id = a.role_id WHERE a.username = :username
                """)
                .param("username", username)
                .query((row, index) -> new Account(row.getLong("id"), row.getString("username"),
                        row.getString("password_hash"), row.getString("full_name"), row.getString("role_code"),
                        row.getString("status"), Set.of()))
                .optional()
                .map(account -> {
                    var permissions = jdbc.sql("""
                            SELECT p.code FROM permissions p
                            JOIN role_permissions rp ON rp.permission_id = p.id
                            JOIN accounts a ON a.role_id = rp.role_id WHERE a.id = :id
                            """).param("id", account.id()).query(String.class).list();
                    return new Account(account.id(), account.username(), account.passwordHash(), account.fullName(),
                            account.roleCode(), account.status(), Set.copyOf(permissions));
                });
    }
}
