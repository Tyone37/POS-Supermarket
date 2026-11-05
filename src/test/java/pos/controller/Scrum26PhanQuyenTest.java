package pos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

class Scrum26PhanQuyenTest extends SecurityTestSupport {
    @Test
    void chuaDangNhapKhongDuocVaoTrangRieng() throws Exception {
        mvc.perform(get("/workspace/cashier")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void cookiePhienKhongConHopLeChuyenVeDangNhap() throws Exception {
        // Mô phỏng cookie của phiên không còn tồn tại, không chờ hết hạn thật 30 phút.
        mvc.perform(get("/workspace/cashier").cookie(new Cookie("JSESSIONID", "phien-cu")))
                .andExpect(redirectedUrlPattern("**/login"))
                .andExpect(unauthenticated());
    }

    @Test
    void thuNganChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/workspace/cashier"));
        mvc.perform(get("/workspace/cashier").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/shifts"));
    }

    @Test
    void quanLyChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("manager").roles("MANAGER")))
                .andExpect(redirectedUrl("/workspace/manager"));
        mvc.perform(get("/workspace/manager").with(user("manager").roles("MANAGER")))
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void adminChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/workspace/admin"));
        mvc.perform(get("/workspace/admin").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/accounts"));
    }

    @Test
    void thuNganKhongDuocXemTrangTaiKhoan() throws Exception {
        mvc.perform(get("/accounts").with(user("cashier").roles("CASHIER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void thuNganKhongDuocGuiLenhTaoTaiKhoan() throws Exception {
        mvc.perform(post("/accounts").with(user("cashier").roles("CASHIER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void quanLyKhongDuocVaoTrangBanHang() throws Exception {
        mvc.perform(get("/sales").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void quanLyKhongDuocGuiLenhMoCa() throws Exception {
        mvc.perform(post("/shifts").with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminKhongDuocVaoTrangSanPham() throws Exception {
        mvc.perform(get("/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminKhongDuocGuiLenhThemSanPham() throws Exception {
        mvc.perform(post("/products").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
