package pos.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pos.config.SecurityConfig;
import pos.entity.Account;
import pos.repository.AccountRepository;
import pos.service.CustomUserDetailsService;

import java.util.Optional;
import java.util.Set;

// Kiểm tra request và trang HTML với Security thật, chỉ giả dữ liệu tài khoản.
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {UiViewAdvice.class, BusinessErrorHandler.class}))
@Import({SecurityConfig.class, CustomUserDetailsService.class})
abstract class SecurityTestSupport {
    @Autowired protected MockMvc mvc;
    @MockitoBean protected AccountRepository accounts;

    @BeforeEach
    void setupSecurity() {
        var encoder = new SecurityConfig().passwordEncoder();
        String hash = encoder.encode("demo123");
        when(accounts.findByUsername("cashier"))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        1L,
                                        "cashier",
                                        hash,
                                        "Thu ngân",
                                        "CASHIER",
                                        "ACTIVE",
                                        Set.of())));
        when(accounts.findByUsername("manager"))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        2L, "manager", hash, "Quản lý", "MANAGER", "ACTIVE",
                                        Set.of())));
        when(accounts.findByUsername("admin"))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        3L,
                                        "admin",
                                        hash,
                                        "Quản trị",
                                        "ADMIN",
                                        "ACTIVE",
                                        Set.of())));
        when(accounts.findByUsername("locked"))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        4L, "locked", hash, "Đã khóa", "CASHIER", "LOCKED",
                                        Set.of())));
    }
}
