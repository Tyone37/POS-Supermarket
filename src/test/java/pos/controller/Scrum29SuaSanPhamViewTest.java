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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pos.config.PosTime;
import pos.config.SecurityConfig;
import pos.dto.ProductEditForm;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductEditController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class})
class Scrum29SuaSanPhamViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private CatalogService catalog;
    private Map<String, Object> product;

    @BeforeEach
    void setup() {
        for (String role : List.of("MANAGER", "CASHIER", "ADMIN")) {
            when(accounts.findByUsername(role)).thenReturn(Optional.of(new Account(1L, role, "unused", "Quản lý",
                    role, "ACTIVE", Set.of())));
        }
        product = new HashMap<>(Map.of("id", 20L, "code", "SP20", "barcode", "001234", "name", "Tên cũ",
                "category_id", 2L, "unit_id", 1L, "sale_price", new BigDecimal("20000"),
                "stock_quantity", new BigDecimal("10"), "reserved_quantity", BigDecimal.ZERO,
                "updated_at", Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"))));
        product.put("quantity_scale", 0);
        product.put("image_url", null);
        product.put("unit_locked", false);
        when(catalog.editable("MANAGER", 20)).thenReturn(product);
        when(catalog.categories()).thenReturn(List.of(Map.of("id", 2L, "name", "Nhóm hàng")));
        when(catalog.units()).thenReturn(List.of(Map.of("id", 1L, "name", "Cái")));
    }

    private MockHttpServletRequestBuilder request(boolean saving, String role) {
        var request = saving ? post("/products/20") : get("/products/20/edit");
        return request.with(user(role).roles(role)).flashAttr("uiDisplayName", "Quản lý").flashAttr("uiInitials", "QL");
    }

    private ProductEditForm form() {
        var form = new ProductEditForm();
        form.setBarcode("001234");
        form.setName("Tên mới");
        form.setCategoryId(2L);
        form.setUnitId(1L);
        form.setPrice("25000");
        form.setStock("10");
        form.setExpectedUpdatedAt("2026-11-25T00:00:00Z");
        return form;
    }

    @Test
    void hienThiDuLieuCuVaFormLuuCoCsrf() throws Exception {
        mvc.perform(request(false, "MANAGER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("SP20")))
                .andExpect(content().string(containsString("value=\"001234\"")))
                .andExpect(content().string(containsString("value=\"20000\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/products/20\"")));
    }

    @Test
    void luuThanhCongQuayVeDanhSach() throws Exception {
        var form = form();
        mvc.perform(request(true, "MANAGER").with(csrf()).flashAttr("form", form))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"));
        verify(catalog).update("MANAGER", 20, form);
    }

    @ParameterizedTest
    @CsvSource({"name", "barcode", "price", "stock", "expectedUpdatedAt"})
    void thieuTruongBatBuocBaoLoiVaGiuGiaTri(String field) throws Exception {
        var form = form();
        switch (field) {
            case "name" -> form.setName("");
            case "barcode" -> form.setBarcode("");
            case "price" -> form.setPrice("");
            case "stock" -> form.setStock("");
            case "expectedUpdatedAt" -> form.setExpectedUpdatedAt("");
            default -> throw new IllegalArgumentException(field);
        }
        mvc.perform(request(true, "MANAGER").with(csrf()).flashAttr("form", form))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("form", field))
                .andExpect(model().attribute("form", form));
        verify(catalog, never()).update(eq("MANAGER"), eq(20L), any());
    }

    @Test
    void nhomVaDonViBatBuoc() throws Exception {
        var form = form();
        form.setCategoryId(null);
        form.setUnitId(null);
        mvc.perform(request(true, "MANAGER").with(csrf()).flashAttr("form", form))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("form", "categoryId", "unitId"));
    }

    @Test
    void trungMaTaiDatabaseBaoLoiTruongMaVach() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate")).when(catalog).update(eq("MANAGER"), eq(20L), any());
        mvc.perform(request(true, "MANAGER").with(csrf()).flashAttr("form", form())).andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("form", "barcode"))
                .andExpect(content().string(containsString("Mã vạch hoặc SKU đã tồn tại")));
    }

    @Test
    void loiNghiepVuGiuGiaNhapDeSua() throws Exception {
        var form = form();
        form.setPrice("-1");
        doThrow(new BusinessException("Đơn giá phải lớn hơn 0.")).when(catalog).update(eq("MANAGER"), eq(20L), any());
        mvc.perform(request(true, "MANAGER").with(csrf()).flashAttr("form", form)).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Đơn giá phải lớn hơn 0.")))
                .andExpect(content().string(containsString("value=\"-1\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASHIER", "ADMIN"})
    void vaiTroKhongQuanLyBiCamCaGetVaPost(String role) throws Exception {
        mvc.perform(request(false, role)).andExpect(status().isForbidden());
        mvc.perform(request(true, role).with(csrf()).flashAttr("form", form())).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void khongCsrfKhongDuocLuu() throws Exception {
        mvc.perform(request(true, "MANAGER").flashAttr("form", form())).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void chuaDangNhapPhaiDangNhap() throws Exception {
        mvc.perform(get("/products/20/edit")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void donViLichSuBiKhoaTrenForm() throws Exception {
        product.put("unit_locked", true);
        mvc.perform(request(false, "MANAGER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(containsString("đơn vị được giữ nguyên")));
    }
}
