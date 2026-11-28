package pos.controller;

import java.math.BigDecimal;
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

import pos.config.PosTime;
import pos.config.SecurityConfig;
import pos.config.TransferSettings;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PosController.class, UiNavigationController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class})
class Scrum51KhoaCaViewTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AccountRepository accounts;
    @MockitoBean
    private AccountAdminService accountAdmin;
    @MockitoBean
    private CatalogService catalog;
    @MockitoBean
    private SaleService sales;
    @MockitoBean
    private ShiftService shifts;
    @MockitoBean
    private TransferSettings transfer;

    @BeforeEach
    void setup() {
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(new Account(
                1L, "cashier", "unused", "Thu ngân", "CASHIER", "ACTIVE", Set.of())));
        when(shifts.current("cashier")).thenReturn(Optional.of(Map.of("id", 20L, "status", "CLOSED")));
        when(sales.view("cashier", 10L)).thenReturn(Map.of("id", 10L, "status", "DRAFT", "shift_open", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sales", "/sales/workspace", "/sales/10", "/sales/10/checkout"})
    void caDongYeuCauMoCaMoi(String path) throws Exception {
        mvc.perform(get(path).with(user("cashier").roles("CASHIER")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shifts"));
        verify(sales, never()).items(any(), eq(10L));
        verifyNoInteractions(catalog);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sales", "/sales/workspace"})
    void chuaCoCaYeuCauMoCa(String path) throws Exception {
        when(shifts.current("cashier")).thenReturn(Optional.empty());
        mvc.perform(get(path).with(user("cashier").roles("CASHIER")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shifts"));
        verifyNoInteractions(sales);
    }

    @Test
    void coCaMoVanVaoDonNhapCuaCaMoi() throws Exception {
        when(shifts.current("cashier")).thenReturn(Optional.of(Map.of("id", 21L, "status", "OPEN")));
        when(sales.list("cashier")).thenReturn(List.of(
                Map.of("id", 10L, "shift_id", 20L, "status", "DRAFT"),
                Map.of("id", 11L, "shift_id", 21L, "status", "DRAFT")));

        mvc.perform(get("/sales/workspace").with(user("cashier").roles("CASHIER")))
                .andExpect(redirectedUrl("/sales/11"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
    void thanhToanMuonTraLoiKhongHienLaiFormThanhToan(String method) throws Exception {
        doThrow(new BusinessException("Ca đã kết thúc. Vui lòng mở ca mới."))
                .when(sales).pay(eq("cashier"), eq(10L), any());

        mvc.perform(post("/sales/10/pay").with(user("cashier").roles("CASHIER")).with(csrf())
                        .param("method", method).param("received", "50000").param("requestKey", "payment-key-123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ca đã kết thúc. Vui lòng mở ca mới.")));
        verify(sales, never()).items("cashier", 10L);
        verify(sales, never()).payments("cashier", 10L);
    }

    @Test
    void hoaDonCuVanXemDuocChiDoc() throws Exception {
        when(sales.view("cashier", 10L)).thenReturn(Map.of("id", 10L, "status", "PAID", "shift_open", false,
                "order_no", "DH-10", "total_amount", new BigDecimal("40000.00"),
                "subtotal_amount", new BigDecimal("40000.00"), "discount_amount", BigDecimal.ZERO));
        when(sales.items("cashier", 10L)).thenReturn(List.of());
        when(sales.payments("cashier", 10L)).thenReturn(List.of());
        when(sales.invoiceId("cashier", 10L)).thenReturn(51L);
        when(transfer.qrUrl(any(), any())).thenReturn(null);

        mvc.perform(get("/sales/10/checkout").with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/invoices/51")));
    }

    @Test
    void thanhToanThieuCsrfBiTuChoiTruocService() throws Exception {
        mvc.perform(post("/sales/10/pay").with(user("cashier").roles("CASHIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sales);
    }
}
