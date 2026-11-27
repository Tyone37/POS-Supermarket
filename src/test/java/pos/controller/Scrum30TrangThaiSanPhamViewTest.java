package pos.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pos.config.PosTime;
import pos.config.SecurityConfig;
import pos.config.TransferSettings;
import pos.dto.ProductStatusForm;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import pos.service.SaleService;
import pos.service.ShiftService;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductEditController.class, PosController.class}, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, TransferSettings.class, PosTime.class})
class Scrum30TrangThaiSanPhamViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private CatalogService catalog;
    @MockitoBean private AccountAdminService accountAdmin;
    @MockitoBean private SaleService sales;
    @MockitoBean private ShiftService shifts;
    private Map<String,Object> product;
    private static final String REVISION = "2026-11-25T00:00:00Z";

    @BeforeEach
    void setup() {
        for (String role : List.of("MANAGER", "CASHIER", "ADMIN")) {
            when(accounts.findByUsername(role)).thenReturn(Optional.of(new Account(1L, role, "unused", "Người dùng",
                    role, "ACTIVE", Set.of())));
        }
        product = new HashMap<>(Map.of("id", 20L, "code", "SP20", "barcode", "001234", "name", "Sữa tươi",
                "status", "ACTIVE", "sale_price", new BigDecimal("20000"), "stock_quantity", new BigDecimal("10"),
                "unit_name", "Cái", "category_name", "Nhóm hàng", "updated_at", Timestamp.from(Instant.parse(REVISION))));
        product.put("image_url", null);
        when(catalog.search("", null, false)).thenReturn(List.of(product));
    }

    private MockHttpServletRequestBuilder change(String role, String state) {
        return post("/products/20/status").with(user(role).roles(role))
                .param("status", state).param("expectedUpdatedAt", REVISION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ACTIVE"})
    void quanLyGuiDungTrangThaiDichVaQuayVeDanhSach(String state) throws Exception {
        mvc.perform(change("MANAGER", state).with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
        verify(catalog).changeStatus(eq("MANAGER"), eq(20L), argThat(f -> f.getStatus().name().equals(state)
                && f.getExpectedUpdatedAt().equals(REVISION)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "STOPPED"})
    void danhMucHienThiNhanVaNutDungVoiTrangThai(String state) throws Exception {
        product.put("status", state);
        mvc.perform(get("/products").with(user("MANAGER").roles("MANAGER"))
                        .flashAttr("uiDisplayName", "Quản lý").flashAttr("uiInitials", "QL"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Sữa tươi")))
                .andExpect(content().string(containsString("action=\"/products/20/status\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString(state.equals("ACTIVE") ? "Đang bán" : "Ngừng bán")))
                .andExpect(content().string(containsString(state.equals("ACTIVE") ? "value=\"STOPPED\"" : "value=\"ACTIVE\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASHIER", "ADMIN"})
    void vaiTroKhongQuanLyBiTuChoi(String role) throws Exception {
        mvc.perform(change(role, "STOPPED").with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void khongCsrfKhongDoiTrangThai() throws Exception {
        mvc.perform(change("MANAGER", "STOPPED")).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void chuaDangNhapPhaiDangNhap() throws Exception {
        mvc.perform(post("/products/20/status").with(csrf()).param("status", "STOPPED").param("expectedUpdatedAt", REVISION))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(catalog);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DELETED", "stopped", "LOCKED"})
    void trangThaiKhongHopLeKhongGoiService(String state) throws Exception {
        mvc.perform(change("MANAGER", state).with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }

    @Test
    void thieuPhienBanKhongDoiTrangThai() throws Exception {
        mvc.perform(post("/products/20/status").with(user("MANAGER").roles("MANAGER")).with(csrf())
                .param("status", "STOPPED")).andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }

    @Test
    void loiPhienBanCuHienThiRo() throws Exception {
        doThrow(new BusinessException("Sản phẩm đã thay đổi. Vui lòng tải lại."))
                .when(catalog).changeStatus(eq("MANAGER"), eq(20L), any(ProductStatusForm.class));
        mvc.perform(change("MANAGER", "STOPPED").with(csrf())).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Sản phẩm đã thay đổi. Vui lòng tải lại.")));
    }

    @Test
    void getKhongThayDoiTrangThai() throws Exception {
        mvc.perform(get("/products/20/status").with(user("MANAGER").roles("MANAGER")))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(catalog);
    }
}
