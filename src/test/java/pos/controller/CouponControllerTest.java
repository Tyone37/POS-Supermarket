package pos.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
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
import pos.entity.Account;
import pos.repository.AccountRepository;
import pos.service.CouponService;
import pos.service.CustomUserDetailsService;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CouponController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class})
class CouponControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private CouponService coupons;

    @BeforeEach void setup() {
        for (String role : List.of("MANAGER", "CASHIER", "ADMIN")) {
            String name = role.toLowerCase();
            when(accounts.findByUsername(name)).thenReturn(Optional.of(new Account(1L, name, "unused", name, role, "ACTIVE", Set.of())));
        }
        when(coupons.list(anyString(), anyString(), anyInt())).thenReturn(new CouponService.Page(List.of(), 0, false));
    }

    private MockHttpServletRequestBuilder valid() {
        return post("/coupons").with(user("manager").roles("MANAGER")).with(csrf())
                .param("code", "SALE10").param("name", "Giảm giá").param("scope", "ORDER").param("productId", "")
                .param("type", "PERCENT").param("value", "10").param("minimum", "0")
                .param("startsAt", "2026-11-28T09:00").param("endsAt", "2026-11-29T09:00").param("status", "ENABLED");
    }

    @Test void emptyListRendersClearMessage() throws Exception {
        mvc.perform(get("/coupons").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không có coupon phù hợp.")));
    }
    @Test void formContainsBothScopesAndCsrf() throws Exception {
        mvc.perform(get("/coupons/new").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Toàn đơn hàng")))
                .andExpect(content().string(containsString("data-page=\"coupons\"")))
                .andExpect(content().string(containsString("Một sản phẩm"))).andExpect(content().string(containsString("name=\"_csrf\"")));
    }
    @Test void populatedListRendersProductTargetAndEditLink() throws Exception {
        var row = Map.<String, Object>of("id", 5L, "code", "SALE10", "name", "Giảm giá", "scope", "PRODUCT",
                "product_name", "SP7 — Sữa", "discount_value", BigDecimal.TEN, "discount_type", "PERCENT",
                "min_order_amount", BigDecimal.ZERO, "starts_at", Timestamp.valueOf("2026-11-28 02:00:00"),
                "ends_at", Timestamp.valueOf("2026-11-29 02:00:00"));
        var values = new java.util.HashMap<>(row); values.put("lifecycle_status", "ENABLED");
        when(coupons.list("manager", "", 0)).thenReturn(new CouponService.Page(List.of(values), 0, false));
        mvc.perform(get("/coupons").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("SP7 — Sữa")))
                .andExpect(content().string(containsString("/coupons/5/edit")));
    }
    @Test void wholeOrderEmptyProductAccepted() throws Exception {
        mvc.perform(valid()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/coupons"));
        verify(coupons).save(eq("manager"), isNull(), argThat(f -> f.productId() == null && f.scope().equals("ORDER")));
    }
    @Test void productScopeAndTargetBoundCorrectly() throws Exception {
        mvc.perform(valid().with(r -> { r.setParameter("scope", "PRODUCT"); r.setParameter("productId", "7"); return r; }))
                .andExpect(status().is3xxRedirection());
        verify(coupons).save(eq("manager"), isNull(), argThat(f -> Long.valueOf(7).equals(f.productId()) && f.scope().equals("PRODUCT")));
    }
    @Test void editRendersStoredHistoricalConfiguration() throws Exception {
        when(coupons.get("manager", 5)).thenReturn(Map.of("code", "SALE10", "name", "Giảm giá", "scope", "PRODUCT", "product_id", 7L,
                "discount_type", "PERCENT", "discount_value", new BigDecimal("10.00"), "min_order_amount", BigDecimal.ZERO,
                "starts_at", Timestamp.valueOf("2026-11-28 02:00:00"), "ends_at", Timestamp.valueOf("2026-11-29 02:00:00"), "lifecycle_status", "ENABLED"));
        when(coupons.products("manager")).thenReturn(List.of(Map.of("id", 7L, "code", "SP7", "name", "Sữa", "status", "ACTIVE")));
        mvc.perform(get("/coupons/5/edit").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(containsString("SP7 — Sữa")));
    }
    @Test void badProductIdShowsFormError() throws Exception {
        mvc.perform(valid().with(r -> { r.setParameter("productId", "abc"); return r; })).andExpect(status().isBadRequest())
                .andExpect(view().name("pos/coupon-form")).andExpect(model().attributeHasFieldErrors("form", "productId"));
        verify(coupons, never()).save(any(), any(), any());
    }
    @Test void requiredNameErrorRetainsForm() throws Exception {
        mvc.perform(valid().with(r -> { r.setParameter("name", ""); return r; })).andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }
    @Test void businessFailureShownWithoutSavingAgain() throws Exception {
        when(coupons.save(any(), any(), any())).thenThrow(new pos.exception.BusinessException("Mã coupon đã tồn tại."));
        mvc.perform(valid()).andExpect(status().isBadRequest()).andExpect(content().string(containsString("Mã coupon đã tồn tại.")));
    }
    @Test void duplicateRaceShowsFieldError() throws Exception {
        when(coupons.save(any(), any(), any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        mvc.perform(valid()).andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("form", "code"));
    }
    @ParameterizedTest @ValueSource(strings = {"CASHIER", "ADMIN"})
    void otherRolesCannotReadOrWriteManagement(String role) throws Exception {
        mvc.perform(get("/coupons").with(user(role.toLowerCase()).roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/coupons").with(user(role.toLowerCase()).roles(role)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(coupons);
    }
    @Test void writeRequiresCsrf() throws Exception {
        mvc.perform(post("/coupons").with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
        verifyNoInteractions(coupons);
    }
}
