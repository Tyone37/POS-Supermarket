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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pos.config.PosTime;
import pos.config.SecurityConfig;
import pos.config.TransferSettings;
import pos.dto.InvoiceSearch;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import pos.service.InvoiceSearchService;
import pos.service.SaleService;
import pos.service.ShiftService;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {InvoiceSearchController.class, PosController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class, TransferSettings.class})
class Scrum45TraCuuHoaDonViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private InvoiceSearchService invoices;
    @MockitoBean private SaleService sales;
    @MockitoBean private ShiftService shifts;
    @MockitoBean private CatalogService catalog;
    @MockitoBean private AccountAdminService accountAdmin;

    @BeforeEach
    void setup() {
        for (String role : List.of("CASHIER", "MANAGER", "ADMIN", "OTHER")) {
            when(accounts.findByUsername(role)).thenReturn(Optional.of(new Account(
                    7L, role, "unused", "Người dùng", role, "ACTIVE", Set.of())));
        }
        when(invoices.search(anyString(), any(), anyInt())).thenReturn(new InvoiceSearch.Result(List.of(), 0, false));
    }

    private MockHttpServletRequestBuilder request(String path, String role) {
        return get(path).with(user(role).roles(role)).flashAttr("uiDisplayName", "Người dùng").flashAttr("uiInitials", "ND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASHIER", "MANAGER", "ADMIN"})
    void nguoiDuocPhepXemNhanKetQuaRongThanhCong(String role) throws Exception {
        mvc.perform(request("/invoices", role)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có hóa đơn phù hợp")))
                .andExpect(content().string(containsString("href=\"/invoices\"")));
        verify(invoices).search(eq(role), any(), eq(0));
    }

    @Test
    void chuaDangNhapPhaiDangNhap() throws Exception {
        mvc.perform(get("/invoices")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(invoices);
    }

    @Test
    void vaiTroKhongDuocPhepBiCam() throws Exception {
        mvc.perform(request("/invoices", "OTHER")).andExpect(status().isForbidden());
        verifyNoInteractions(invoices);
    }

    @Test
    void taiKhoanKhoaKhongDuocTraCuu() throws Exception {
        when(accounts.findByUsername("CASHIER")).thenReturn(Optional.of(
                new Account(7L, "CASHIER", "unused", "Thu ngân", "CASHIER", "LOCKED", Set.of())));
        mvc.perform(request("/invoices", "CASHIER")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(invoices);
    }

    @ParameterizedTest
    @CsvSource({"method,CARD", "method,cash", "status,PAID", "status,FAILED"})
    void locSaiBaoLoiTaiTruong(String field, String value) throws Exception {
        mvc.perform(request("/invoices", "CASHIER").param(field, value)).andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("filter", field))
                .andExpect(content().string(not(containsString("Không có hóa đơn phù hợp"))));
        verifyNoInteractions(invoices);
    }

    @Test
    void maQuaDaiBaoLoi() throws Exception {
        mvc.perform(request("/invoices", "CASHIER").param("code", "X".repeat(31)))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("filter", "code"));
        verifyNoInteractions(invoices);
    }

    @Test
    void ketHopBoLocHienThiVaGiuKhiChuyenTrang() throws Exception {
        var row = new InvoiceSearch.Row(81L, "HD-81", time(), "Thu ngân cũ", "BANK_TRANSFER", "ISSUED", new BigDecimal("6000"));
        when(invoices.search(anyString(), any(), eq(1))).thenReturn(new InvoiceSearch.Result(List.of(row), 1, true));
        mvc.perform(request("/invoices", "CASHIER").param("code", "HD-").param("method", "BANK_TRANSFER")
                        .param("status", "ISSUED").param("page", "1").param("issuedBy", "999").param("role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/invoices/81")))
                .andExpect(content().string(containsString("HD-81")))
                .andExpect(content().string(containsString("25/11/2026 07:00:00")))
                .andExpect(content().string(containsString("6.000 VNĐ")))
                .andExpect(content().string(containsString("Chuyển khoản")))
                .andExpect(content().string(containsString("page=2")))
                .andExpect(content().string(containsString("code=HD-")))
                .andExpect(content().string(containsString("method=BANK_TRANSFER")))
                .andExpect(content().string(containsString("status=ISSUED")));
        verify(invoices).search(eq("CASHIER"), argThat(f -> f.getCode().equals("HD-")
                && f.getMethod().equals("BANK_TRANSFER") && f.getStatus().equals("ISSUED")), eq(1));
        verifyNoInteractions(sales);
    }

    @Test
    void loiTrangDuocHienThiKhongThanhKetQuaRong() throws Exception {
        when(invoices.search(anyString(), any(), eq(-1))).thenThrow(new BusinessException("Trang kết quả không hợp lệ."));
        mvc.perform(request("/invoices", "MANAGER").param("page", "-1")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Trang kết quả không hợp lệ.")))
                .andExpect(content().string(not(containsString("Không có hóa đơn phù hợp"))));
    }

    @Test
    void trangKhongPhaiSoBiTuChoi() throws Exception {
        mvc.perform(request("/invoices", "CASHIER").param("page", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(invoices);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISSUED", "VOID"})
    void lienKetMoDungHoaDonVaTrangThai(String invoiceStatus) throws Exception {
        var invoice = new HashMap<String, Object>(Map.of("invoice_no", "HD-81", "issued_at", time(),
                "cashier_name", "Thu ngân", "method", "CASH", "status", invoiceStatus, "items", List.of()));
        for (String field : List.of("subtotal_amount", "discount_amount", "total_amount", "amount_received", "change_amount")) {
            invoice.put(field, BigDecimal.ZERO);
        }
        invoice.put("bank_reference", null);
        when(sales.invoice("MANAGER", 81)).thenReturn(invoice);
        mvc.perform(request("/invoices/81", "MANAGER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("HD-81")))
                .andExpect(content().string(containsString(invoiceStatus.equals("VOID") ? "Đã hủy" : "Đã thanh toán")))
                .andExpect(content().string(containsString("Tra cứu hóa đơn")))
                .andExpect(content().string(not(containsString("Bán đơn mới"))));
        verify(sales).invoice("MANAGER", 81);
        verifyNoInteractions(invoices);
    }

    @Test
    void traCuuKhongCoHanhDongGhiDuLieu() throws Exception {
        mvc.perform(post("/invoices").with(user("CASHIER").roles("CASHIER")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(invoices, sales);
    }

    private Timestamp time() {
        return Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"));
    }
}
