package pos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class Scrum25DangXuatTest extends SecurityTestSupport {
    @Test
    void dangXuatHuyPhienVaXoaCookie() throws Exception {
        var result =
                mvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("username", "cashier")
                                        .param("password", "demo123"))
                        .andExpect(authenticated().withUsername("cashier"))
                        .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(unauthenticated());
        assertTrue(session.isInvalid());
        mvc.perform(get("/workspace/cashier"))
                .andExpect(redirectedUrlPattern("**/login"))
                .andExpect(unauthenticated());
    }

    @Test
    void dangXuatKhongCoCsrfBiTuChoi() throws Exception {
        mvc.perform(post("/logout").with(user("cashier").roles("CASHIER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void giaoDienHienThongBaoDangXuat() throws Exception {
        mvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã đăng xuất")));
    }
}
