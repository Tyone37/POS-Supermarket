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
import pos.dto.Forms;
import pos.dto.InvoicePrint;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import pos.service.InvoicePrintService;
import pos.service.SaleService;
import pos.service.ShiftService;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = {InvoicePrintController.class, PosController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, TransferSettings.class, PosTime.class})
class Scrum46InHoaDonViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private InvoicePrintService printing;
    @MockitoBean private SaleService sales;
    @MockitoBean private ShiftService shifts;
    @MockitoBean private CatalogService catalog;
    @MockitoBean private AccountAdminService accountAdmin;

    @BeforeEach
    void setup() {
        for (String role : List.of("CASHIER", "MANAGER", "ADMIN", "OTHER")) {
            when(accounts.findByUsername(role)).thenReturn(Optional.of(new Account(
                    7L, role, "unused", "Thu ngân", role, "ACTIVE", Set.of())));
        }
        when(printing.preview(anyString(), eq(81L))).thenReturn(invoice("CASH", "ISSUED", "Tên hàng lúc bán"));
    }

    private MockHttpServletRequestBuilder request(String path, String role) {
        return get(path).with(user(role).roles(role)).flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASHIER", "MANAGER", "ADMIN"})
    void duocXemTruocVaMacDinhGiayCuon(String role) throws Exception {
        mvc.perform(request("/invoices/81/print", role)).andExpect(status().isOk())
                .andExpect(content().string(containsString("invoice-print-thermal.css")))
                .andExpect(content().string(containsString("Giấy cuộn 80 mm")))
                .andExpect(content().string(not(containsString("A4"))))
                .andExpect(content().string(not(containsString("<select"))))
                .andExpect(content().string(containsString("data-print-invoice")))
                .andExpect(content().string(containsString("/invoices/81")))
                .andExpect(content().string(not(containsString("method=\"post\""))));
        verify(printing).preview(role, 81);
        verifyNoInteractions(sales);
    }

    @Test
    void giayCuonCoDayDuDuLieuLichSu() throws Exception {
        mvc.perform(request("/invoices/81/print", "CASHIER").param("paper", "THERMAL")
                        .param("total", "999999999").param("unitPrice", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invoice-print-thermal.css")))
                .andExpect(content().string(containsString("HD-81")))
                .andExpect(content().string(containsString("25/11/2026 08:00:00")))
                .andExpect(content().string(containsString("Tên hàng lúc bán")))
                .andExpect(content().string(containsString("SP-CU · KG")))
                .andExpect(content().string(containsString("1.25")))
                .andExpect(content().string(containsString("9.876,54")))
                .andExpect(content().string(containsString("345,67")))
                .andExpect(content().string(containsString("12.000,00")))
                .andExpect(content().string(containsString("12.345,67")))
                .andExpect(content().string(containsString("Tiền mặt")))
                .andExpect(content().string(not(containsString("999999999"))));
    }

    @Test
    void phuongThucChuyenKhoanVaMaNganHang() throws Exception {
        when(printing.preview("CASHIER", 81)).thenReturn(invoice("BANK_TRANSFER", "ISSUED", "Hàng"));
        mvc.perform(request("/invoices/81/print", "CASHIER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Chuyển khoản")))
                .andExpect(content().string(containsString("FT-LICH-SU")));
    }

    @Test
    void hoaDonVoidCoDanhDauRoTrenBanIn() throws Exception {
        when(printing.preview("CASHIER", 81)).thenReturn(invoice("CASH", "VOID", "Hàng"));
        mvc.perform(request("/invoices/81/print", "CASHIER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("ĐÃ HỦY / VÔ HIỆU")));
    }

    @Test
    void tenSanPhamKhongDuocThucThiThanhScript() throws Exception {
        when(printing.preview("CASHIER", 81)).thenReturn(invoice("CASH", "ISSUED", "<script>alert(1)</script>"));
        mvc.perform(request("/invoices/81/print", "CASHIER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void chuaDangNhapPhaiDangNhap() throws Exception {
        mvc.perform(get("/invoices/81/print")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(printing);
    }

    @Test
    void vaiTroKhacBiCam() throws Exception {
        mvc.perform(request("/invoices/81/print", "OTHER")).andExpect(status().isForbidden());
        verifyNoInteractions(printing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A4", "A3", "thermal", "INVALID"})
    void khoGiayKhongHopLeBiTuChoi(String paper) throws Exception {
        mvc.perform(request("/invoices/81/print", "CASHIER").param("paper", paper)).andExpect(status().isBadRequest());
        verifyNoInteractions(printing);
    }

    @Test
    void khongCoHoaDonTraLoiKhongTaoGiaoDich() throws Exception {
        when(printing.preview("CASHIER", 999)).thenThrow(new BusinessException("Hóa đơn không tồn tại."));
        mvc.perform(request("/invoices/999/print", "CASHIER")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Hóa đơn không tồn tại.")));
        verifyNoInteractions(sales);
    }

    @Test
    void khongCoEndpointGhiKhiIn() throws Exception {
        mvc.perform(post("/invoices/81/print").with(user("CASHIER").roles("CASHIER")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(printing, sales);
    }

    @Test
    void thanhToanThanhCongVanMoDungHoaDonDaLuu() throws Exception {
        when(sales.pay(eq("CASHIER"), eq(10L), any(Forms.Payment.class))).thenReturn(81L);
        mvc.perform(post("/sales/10/pay").with(user("CASHIER").roles("CASHIER")).with(csrf())
                        .param("method", "CASH").param("received", "20000").param("requestKey", "request-123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/invoices/81"));
        verifyNoInteractions(printing);
    }

    @Test
    void chiTietHoaDonCoLinkXemTruocDeInLai() throws Exception {
        var saved = new HashMap<String, Object>(Map.of("id", 81L, "invoice_no", "HD-81", "issued_at", time(),
                "cashier_name", "Thu ngân", "method", "CASH", "status", "ISSUED", "items", List.of()));
        saved.put("bank_reference", null);
        for (String key : List.of("subtotal_amount", "discount_amount", "total_amount", "amount_received", "change_amount")) {
            saved.put(key, BigDecimal.ZERO);
        }
        when(sales.invoice("CASHIER", 81)).thenReturn(saved);
        mvc.perform(request("/invoices/81", "CASHIER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/invoices/81/print")))
                .andExpect(content().string(not(containsString("onclick="))));
    }

    private Timestamp time() {
        return Timestamp.from(Instant.parse("2026-11-25T01:00:00Z"));
    }

    private InvoicePrint invoice(String method, String status, String product) {
        var line = new InvoicePrint.Line("SP-CU", product, "KG", new BigDecimal("1.250"),
                new BigDecimal("9876.54"), new BigDecimal("345.67"), new BigDecimal("12000.00"));
        return new InvoicePrint(81, "HD-81", time(), "Tên lúc bán", method, status,
                method.equals("BANK_TRANSFER") ? "FT-LICH-SU" : null, new BigDecimal("12345.67"),
                new BigDecimal("345.67"), new BigDecimal("12000.00"), new BigDecimal("20000"),
                new BigDecimal("8000"), List.of(line));
    }
}
