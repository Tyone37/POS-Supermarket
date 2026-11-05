package pos;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pos.entity.Account;
import pos.repository.AccountRepository;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
class AuthenticationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private AccountRepository accounts;

    @BeforeEach
    void prepareAccounts() {
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(account("cashier", "CASHIER", "ACTIVE")));
        when(accounts.findByUsername("manager")).thenReturn(Optional.of(account("manager", "MANAGER", "ACTIVE")));
        when(accounts.findByUsername("admin")).thenReturn(Optional.of(account("admin", "ADMIN", "ACTIVE")));
        when(accounts.findByUsername("locked")).thenReturn(Optional.of(account("locked", "CASHIER", "LOCKED")));
        when(accounts.findByUsername("missing")).thenReturn(Optional.empty());
    }

    private Account account(String name, String role, String state) {
        return new Account(1L, name, encoder.encode("test-password"), "Test User", role, state, Set.of("SALE_CREATE"));
    }

    @Test
    void loginPage_shouldRenderFormWithCsrf() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    }

    @Test
    void login_shouldAuthenticateActiveCashierWithPermissions() throws Exception {
        mvc.perform(formLogin().user("cashier").password("test-password"))
                .andExpect(authenticated().withAuthorities(Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CASHIER"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SALE_CREATE"))))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void login_shouldAuthenticateManager() throws Exception {
        mvc.perform(formLogin().user("manager").password("test-password"))
                .andExpect(authenticated().withAuthorities(Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SALE_CREATE"))));
    }

    @Test
    void login_shouldRejectWrongPassword() throws Exception {
        mvc.perform(formLogin().user("cashier").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=invalid"));
    }

    @Test
    void login_shouldRejectUnknownUsername() throws Exception {
        mvc.perform(formLogin().user("missing").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=invalid"));
    }

    @Test
    void login_shouldRejectLockedAccount() throws Exception {
        mvc.perform(formLogin().user("locked").password("test-password"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=locked"));
    }

    @Test
    void login_shouldNotRevealLockStatusForWrongPassword() throws Exception {
        mvc.perform(formLogin().user("locked").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=invalid"));
    }

    @Test
    void protectedPage_shouldRequireLogin() throws Exception {
        mvc.perform(get("/workspace/cashier")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void login_shouldRequireCsrfToken() throws Exception {
        mvc.perform(post("/login").param("username", "cashier").param("password", "test-password"))
                .andExpect(status().isForbidden()).andExpect(unauthenticated());
    }

    @Test
    void home_shouldRouteEachRole() throws Exception {
        mvc.perform(get("/").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/workspace/cashier"));
        mvc.perform(get("/").with(user("manager").roles("MANAGER")))
                .andExpect(redirectedUrl("/workspace/manager"));
        mvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/workspace/admin"));
    }

    @Test
    void cashier_shouldNotAccessAdminWorkspace() throws Exception {
        mvc.perform(get("/workspace/admin").with(user("cashier").roles("CASHIER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_shouldInvalidateAuthenticatedSession() throws Exception {
        var result = mvc.perform(formLogin().user("cashier").password("test-password"))
                .andExpect(authenticated()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        mvc.perform(get("/workspace/cashier")).andExpect(status().is3xxRedirection());
    }
}
