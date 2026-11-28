package pos.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import pos.dto.ShiftInvoice;
import pos.dto.ShiftSummary;
import pos.entity.Account;
import pos.repository.AccountRepository;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.CustomUserDetailsService;
import pos.service.SaleService;
import pos.service.ShiftService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PosController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, TransferSettings.class, PosTime.class})
class Scrum49TomTatCaViewTest {
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

    @BeforeEach
    void setup() {
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(new Account(
                1L, "cashier", "unused", "Thu ngân", "CASHIER", "ACTIVE", Set.of())));
    }

    @Test
    void hienThiSoKhongVaTrangThaiChuaCoHoaDon() throws Exception {
        when(shifts.summary("cashier")).thenReturn(
                new ShiftSummary(shift(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO));

        mvc.perform(get("/shifts").with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa có hóa đơn trong ca")))
                .andExpect(content().string(containsString("0 hóa đơn")));
    }

    @Test
    void hienThiGioMoTienDauCaVaDoanhThuTuMayChu() throws Exception {
        var invoices = List.of(
                new ShiftInvoice(100L, "HD-100", "Thu ngân", openedAt(), new BigDecimal("77600.00"), "CASH"),
                new ShiftInvoice(101L, "HD-101", "Thu ngân", openedAt(), new BigDecimal("24000.00"), "BANK_TRANSFER"));
        when(shifts.summary("cashier")).thenReturn(new ShiftSummary(shift(), invoices,
                new BigDecimal("101600.00"), new BigDecimal("77600.00")));

        mvc.perform(get("/shifts").with(user("cashier").roles("CASHIER"))
                        .param("shiftId", "999").param("revenue", "99999999")
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("08:00")))
                .andExpect(content().string(containsString("24/11/2026")))
                .andExpect(content().string(containsString("500.000")))
                .andExpect(content().string(containsString("101.600")))
                .andExpect(content().string(containsString("2 hóa đơn")))
                .andExpect(content().string(containsString("HD-100")))
                .andExpect(content().string(containsString("HD-101")));
        verify(shifts).summary("cashier");
    }

    @Test
    void chuaMoCaVanHienThiTrangBinhThuong() throws Exception {
        when(shifts.summary("cashier")).thenReturn(
                new ShiftSummary(null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));

        mvc.perform(get("/shifts").with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa có ca đang mở")))
                .andExpect(content().string(containsString("Sẵn sàng cho ca mới")));
    }

    @Test
    void vaiTroKhacKhongDuocVaoTomTatCuaThuNgan() throws Exception {
        when(accounts.findByUsername("manager")).thenReturn(Optional.of(new Account(
                2L, "manager", "unused", "Quản lý", "MANAGER", "ACTIVE", Set.of())));

        mvc.perform(get("/shifts").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shifts);
    }

    private Map<String, Object> shift() {
        return Map.of("id", 30L, "status", "OPEN", "shift_no", "CA-30", "register_name", "Quầy 1",
                "opening_cash", new BigDecimal("500000.00"), "opened_at", openedAt());
    }

    private Timestamp openedAt() {
        return Timestamp.from(Instant.parse("2026-11-24T01:00:00Z"));
    }
}
