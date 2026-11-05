package pos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import pos.service.CustomUserDetailsService;

import java.util.Optional;
import java.util.Set;

class Scrum24DangNhapTest extends SecurityTestSupport {
    private AccountRepository loginAccounts;
    private AuthenticationProvider login;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setupLogin() {
        loginAccounts = mock(AccountRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        CustomUserDetailsService users = new CustomUserDetailsService(loginAccounts);
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
        when(loginAccounts.findByUsername("cashier")).thenReturn(Optional.of(cashier));
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
        when(loginAccounts.findByUsername("locked")).thenReturn(Optional.of(locked));
        var request = UsernamePasswordAuthenticationToken.unauthenticated("locked", "demo123");
        assertThrows(DisabledException.class, () -> login.authenticate(request));
    }

    @Test
    void saiMatKhauChuyenVeTrangBaoLoi() throws Exception {
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "cashier")
                                .param("password", "sai"))
                .andExpect(redirectedUrl("/login?error=invalid"))
                .andExpect(unauthenticated());
    }

    @Test
    void taiKhoanKhoaChuyenVeTrangBaoLoi() throws Exception {
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "locked")
                                .param("password", "demo123"))
                .andExpect(redirectedUrl("/login?error=locked"))
                .andExpect(unauthenticated());
    }

    @Test
    void giaoDienHienLoiSaiThongTinDangNhap() throws Exception {
        mvc.perform(get("/login").param("error", "invalid"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Tên đăng nhập hoặc mật khẩu không chính xác")));
    }

    @Test
    void giaoDienHienLoiTaiKhoanKhoa() throws Exception {
        mvc.perform(get("/login").param("error", "locked"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tài khoản đã bị khóa")));
    }
}
