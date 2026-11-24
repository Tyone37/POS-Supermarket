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
    // Yêu cầu đăng nhập trước khi vào trang riêng.
    @Test
    void chuaDangNhapKhongDuocVaoTrangRieng() throws Exception {
        mvc.perform(get("/workspace/cashier")).andExpect(redirectedUrlPattern("**/login"));
    }

    // Chuyển về đăng nhập khi cookie phiên không hợp lệ.
    @Test
    void cookiePhienKhongConHopLeChuyenVeDangNhap() throws Exception {
        // Mô phỏng cookie của phiên không còn tồn tại, không chờ hết hạn thật 30 phút.
        mvc.perform(get("/workspace/cashier").cookie(new Cookie("JSESSIONID", "phien-cu")))
                .andExpect(redirectedUrlPattern("**/login"))
                .andExpect(unauthenticated());
    }

    // Điều hướng thu ngân đến đúng trang.
    @Test
    void thuNganChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/workspace/cashier"));
        mvc.perform(get("/workspace/cashier").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/shifts"));
    }

    // Điều hướng quản lý đến đúng trang.
    @Test
    void quanLyChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("manager").roles("MANAGER")))
                .andExpect(redirectedUrl("/workspace/manager"));
        mvc.perform(get("/workspace/manager").with(user("manager").roles("MANAGER")))
                .andExpect(redirectedUrl("/products"));
    }

    // Điều hướng quản trị viên đến đúng trang.
    @Test
    void adminChuyenVeDungTrang() throws Exception {
        mvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/workspace/admin"));
        mvc.perform(get("/workspace/admin").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/accounts"));
    }

    // Chặn thu ngân xem trang tài khoản.
    @Test
    void thuNganKhongDuocXemTrangTaiKhoan() throws Exception {
        mvc.perform(get("/accounts").with(user("cashier").roles("CASHIER")))
                .andExpect(status().isForbidden());
    }

    // Chặn thu ngân gửi yêu cầu tạo tài khoản.
    @Test
    void thuNganKhongDuocGuiLenhTaoTaiKhoan() throws Exception {
        mvc.perform(post("/accounts").with(user("cashier").roles("CASHIER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // Chặn quản lý vào trang bán hàng.
    @Test
    void quanLyKhongDuocVaoTrangBanHang() throws Exception {
        mvc.perform(get("/sales").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    // Chặn quản lý gửi yêu cầu mở ca.
    @Test
    void quanLyKhongDuocGuiLenhMoCa() throws Exception {
        mvc.perform(post("/shifts").with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // Chặn quản trị viên vào trang sản phẩm.
    @Test
    void adminKhongDuocVaoTrangSanPham() throws Exception {
        mvc.perform(get("/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // Chặn quản trị viên gửi yêu cầu thêm sản phẩm.
    @Test
    void adminKhongDuocGuiLenhThemSanPham() throws Exception {
        mvc.perform(post("/products").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
