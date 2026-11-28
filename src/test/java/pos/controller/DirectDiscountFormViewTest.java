package pos.controller;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import pos.dto.Forms;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import pos.service.SaleService;
import pos.service.ShiftService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PosController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class, TransferSettings.class})
class DirectDiscountFormViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private SaleService sales;
    @MockitoBean private CatalogService catalog;
    @MockitoBean private ShiftService shifts;
    @MockitoBean private AccountAdminService accountAdmin;

    @BeforeEach
    void setup() {
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(
                new Account(1L, "cashier", "unused", "Thu ngân", "CASHIER", "ACTIVE", Set.of())));
    }

    private MockHttpServletRequestBuilder request() {
        return post("/sales/10/discount/direct").with(user("cashier").roles("CASHIER")).with(csrf());
    }

    @Test
    void toanDonBoTrongDongHangQuanLyVaKhongGuiCheckboxVanNhanDuoc() throws Exception {
        mvc.perform(request().param("scope", "ORDER").param("type", "PERCENT").param("value", "5")
                        .param("itemId", "").param("managerUsername", "").param("managerPassword", ""))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/sales/10"));
        var form = ArgumentCaptor.forClass(Forms.DirectDiscount.class);
        verify(sales).applyDirectDiscount(eq("cashier"), eq(10L), form.capture());
        assertEquals("ORDER", form.getValue().scope());
        assertNull(form.getValue().itemId());
        assertNull(form.getValue().confirmed());
        assertEquals("5", form.getValue().value());
    }

    @Test
    void khongGuiCacTruongTuyChonKhongGay400() throws Exception {
        mvc.perform(request().param("value", "5")).andExpect(status().is3xxRedirection());
        var form = ArgumentCaptor.forClass(Forms.DirectDiscount.class);
        verify(sales).applyDirectDiscount(eq("cashier"), eq(10L), form.capture());
        assertNull(form.getValue().confirmed());
        assertNull(form.getValue().managerUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void checkboxCoGiaTriVanDuocTruyenChinhXac(String confirmed) throws Exception {
        mvc.perform(request().param("scope", "ORDER").param("type", "PERCENT").param("value", "5")
                        .param("confirmed", confirmed)).andExpect(status().is3xxRedirection());
        var form = ArgumentCaptor.forClass(Forms.DirectDiscount.class);
        verify(sales).applyDirectDiscount(eq("cashier"), eq(10L), form.capture());
        assertEquals(Boolean.valueOf(confirmed), form.getValue().confirmed());
    }

    @Test
    void dongHangSaiKieuTraLoiVeGioHangThayViWhitelabel() throws Exception {
        mvc.perform(request().param("scope", "ITEM").param("itemId", "abc").param("value", "5"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/sales/10"))
                .andExpect(flash().attributeExists("error"));
        verifyNoInteractions(sales);
    }

    @Test
    void checkboxSaiKieuTraLoiVeGioHang() throws Exception {
        mvc.perform(request().param("scope", "ORDER").param("value", "5").param("confirmed", "invalid"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        verifyNoInteractions(sales);
    }

    @Test
    void mucGiamBoTrongHienThiLoiNghiepVu() throws Exception {
        doThrow(new BusinessException("Giá trị giảm phải là số dương hợp lệ."))
                .when(sales).applyDirectDiscount(eq("cashier"), eq(10L), any());
        mvc.perform(request().param("scope", "ORDER").param("type", "PERCENT").param("value", ""))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/sales/10"))
                .andExpect(flash().attribute("error", "Giá trị giảm phải là số dương hợp lệ."));
    }
}
