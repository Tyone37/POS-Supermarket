package pos.service;

import java.util.ArrayList;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import pos.repository.AccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;

    public CustomUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + account.roleCode()));
        account.permissions().forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .disabled(!"ACTIVE".equals(account.status()))
                .authorities(authorities)
                .build();
    }
}
