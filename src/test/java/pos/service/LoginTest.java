package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pos.config.SecurityConfig;
import pos.entity.Account;
import pos.repository.AccountRepository;

import java.util.Optional;
import java.util.Set;

class LoginTest {
    private AccountRepository accounts;
    private AuthenticationProvider login;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setup() {
        accounts = mock(AccountRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        CustomUserDetailsService users = new CustomUserDetailsService(accounts);
        login = new SecurityConfig().authenticationProvider(users, encoder);
        Account cashier =
                new Account(
                        1L,
                        "cashier",
                        encoder.encode("demo123"),
                        "Thu ngân",
                        "CASHIER",
                        "ACTIVE",
                        Set.of());
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(cashier));
    }

    @Test
    void dangNhapDungTaiKhoanVaMatKhau() {
        var request = UsernamePasswordAuthenticationToken.unauthenticated("cashier", "demo123");
        var result = login.authenticate(request);
        assertTrue(result.isAuthenticated());
        assertEquals("cashier", result.getName());
    }

    @Test
    void dangNhapSaiMatKhau() {
        var request = UsernamePasswordAuthenticationToken.unauthenticated("cashier", "saimatkhau");
        assertThrows(BadCredentialsException.class, () -> login.authenticate(request));
    }

    @Test
    void dangNhapTaiKhoanKhongTonTai() {
        var request = UsernamePasswordAuthenticationToken.unauthenticated("khongtontai", "demo123");
        assertThrows(BadCredentialsException.class, () -> login.authenticate(request));
    }

    @Test
    void dangNhapBoTrongMatKhau() {
        var request = UsernamePasswordAuthenticationToken.unauthenticated("cashier", "");
        assertThrows(BadCredentialsException.class, () -> login.authenticate(request));
    }

    @Test
    void dangNhapBoTrongTenTaiKhoan() {
        var request = UsernamePasswordAuthenticationToken.unauthenticated("", "demo123");
        assertThrows(BadCredentialsException.class, () -> login.authenticate(request));
    }

    @Test
    void dangNhapTaiKhoanDaKhoa() {
        Account locked =
                new Account(
                        2L,
                        "locked",
                        encoder.encode("demo123"),
                        "Đã khóa",
                        "CASHIER",
                        "LOCKED",
                        Set.of());
        when(accounts.findByUsername("locked")).thenReturn(Optional.of(locked));
        var request = UsernamePasswordAuthenticationToken.unauthenticated("locked", "demo123");
        assertThrows(DisabledException.class, () -> login.authenticate(request));
    }
}
