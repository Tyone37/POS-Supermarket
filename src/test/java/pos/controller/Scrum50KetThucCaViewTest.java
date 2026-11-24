package pos.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pos.config.PosTime;
import pos.config.SecurityConfig;
import pos.config.TransferSettings;
import pos.dto.ShiftClosePreview;
import pos.dto.ShiftSummary;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.CustomUserDetailsService;
import pos.service.ShiftClosingService;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {ShiftClosingController.class, PosController.class}, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class})
class Scrum50KetThucCaViewTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AccountRepository accounts;
    @MockitoBean
    private ShiftClosingService closing;
    @MockitoBean
    private ShiftService shifts;
    @MockitoBean
    private CatalogService catalog;
    @MockitoBean
    private AccountAdminService accountAdmin;
    @MockitoBean
    private SaleService sales;
    @MockitoBean
    private TransferSettings transfer;
    private ShiftClosePreview preview;

    @BeforeEach
    void setup() {
        when(accounts.findByUsername("cashier")).thenReturn(Optional.of(new Account(
                1L, "cashier", "unused", "Thu ngân", "CASHIER", "ACTIVE", Set.of())));
        preview = new ShiftClosePreview(30L, "CA-30", Timestamp.from(Instant.parse("2026-11-25T01:00:00Z")),
                new BigDecimal("500000.00"), 2, new BigDecimal("101600.00"), new BigDecimal("77600.00"),
                new BigDecimal("10000.00"), new BigDecimal("567600.00"), new BigDecimal("560000.00"),
                new BigDecimal("-7600.00"));
        when(closing.preview("cashier", "0")).thenReturn(preview);
        when(closing.preview("cashier", "560000")).thenReturn(preview);
    }

    @Test
    void hienThiFormNhapTienVaKhongDongCa() throws Exception {
        mvc.perform(get("/shifts/close").with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tiền mặt thực tế kiểm đếm")));
        verify(closing, never()).close(any(), any());
    }

    @Test
    void xacNhanHienDayDuSoLieuVaCsrf() throws Exception {
        submitPreview(new MockHttpSession()).andExpect(status().isOk())
                .andExpect(view().name("pos/shift-close-confirm"))
                .andExpect(content().string(containsString("500.000 VNĐ")))
                .andExpect(content().string(containsString("101.600 VNĐ")))
                .andExpect(content().string(containsString("10.000 VNĐ")))
                .andExpect(content().string(containsString("567.600 VNĐ")))
                .andExpect(content().string(containsString("560.000 VNĐ")))
                .andExpect(content().string(containsString("-7.600 VNĐ")))
                .andExpect(content().string(containsString("_csrf")))
                .andExpect(model().attributeExists("confirmationToken"));
        verify(closing, never()).close(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "-1", "abc", "12abc", "1.5", "10000000000000000"})
    void duLieuSaiBaoLoiTaiTruong(String actual) throws Exception {
        mvc.perform(post("/shifts/close").with(user("cashier").roles("CASHIER")).with(csrf())
                        .param("actualCash", actual)
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("pos/shift-close"))
                .andExpect(model().attributeHasFieldErrors("closingForm", "actualCash"))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
        verify(closing, never()).close(any(), any());
    }

    @Test
    void thieuTruongTienThucTeBiTuChoi() throws Exception {
        mvc.perform(post("/shifts/close").with(user("cashier").roles("CASHIER")).with(csrf())
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("closingForm", "actualCash"));
    }

    @Test
    void xacNhanLuuSoLieuMayChuBoQuaTongTienClient() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        confirm(session, token(result)).andExpect(redirectedUrl("/shifts"))
                .andExpect(flash().attributeExists("message"));
        verify(closing).close("cashier", preview);
    }

    @Test
    void guiLapKhongDongCaLanHai() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        confirm(session, token(result)).andExpect(redirectedUrl("/shifts"));
        confirm(session, token(result)).andExpect(status().isBadRequest());
        verify(closing).close("cashier", preview);
    }

    @Test
    void boQuaManXacNhanBiTuChoi() throws Exception {
        confirm(new MockHttpSession(), "forged").andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void tokenKhacPhienBiTuChoi() throws Exception {
        var result = submitPreview(new MockHttpSession()).andReturn();
        confirm(new MockHttpSession(), token(result)).andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void tokenKhacTaiKhoanTrongCungPhienBiTuChoi() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        when(accounts.findByUsername("other")).thenReturn(Optional.of(new Account(
                2L, "other", "unused", "Thu ngân khác", "CASHIER", "ACTIVE", Set.of())));

        mvc.perform(post("/shifts/close/confirm").session(session)
                        .with(user("other").roles("CASHIER")).with(csrf())
                        .param("confirmationToken", token(result)).param("confirmed", "true"))
                .andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void caDongHienThiSnapshotDaLuuThayViTinhLaiDoanhThu() throws Exception {
        var snapshot = new HashMap<String, Object>(Map.of("id", 30L, "status", "CLOSED", "shift_no", "CA-30",
                "register_name", "Quầy 1", "opened_at", preview.openedAt(), "opening_cash", preview.openingCash()));
        snapshot.put("closed_at", Timestamp.from(Instant.parse("2026-11-25T10:30:00Z")));
        snapshot.put("closing_revenue", preview.revenue());
        snapshot.put("closing_cash_sales", preview.cashSales());
        snapshot.put("closing_cash_refunds", preview.cashRefunds());
        snapshot.put("closing_expected_cash", preview.expectedCash());
        snapshot.put("closing_actual_cash", preview.actualCash());
        snapshot.put("closing_difference", preview.difference());
        when(shifts.summary("cashier")).thenReturn(
                new ShiftSummary(snapshot, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));

        mvc.perform(get("/shifts").with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kết quả bàn giao đã lưu")))
                .andExpect(content().string(containsString("567.600 VNĐ")))
                .andExpect(content().string(containsString("560.000 VNĐ")))
                .andExpect(content().string(containsString("-7.600 VNĐ")));
    }

    @Test
    void tokenSaiBiTuChoi() throws Exception {
        var session = new MockHttpSession();
        submitPreview(session);
        confirm(session, "wrong").andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void chuaBamXacNhanBiTuChoi() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        mvc.perform(post("/shifts/close/confirm").session(session)
                        .with(user("cashier").roles("CASHIER")).with(csrf())
                        .param("confirmationToken", token(result)))
                .andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void soLieuThayDoiPhaiNhapVaXacNhanLai() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        doThrow(new BusinessException("Số liệu ca đã thay đổi")).when(closing).close("cashier", preview);
        confirm(session, token(result)).andExpect(redirectedUrl("/shifts/close"))
                .andExpect(flash().attribute("error", "Số liệu ca đã thay đổi"));
    }

    @Test
    void quayLaiSuaTienHuyBanXacNhanCu() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        mvc.perform(get("/shifts/close").session(session).with(user("cashier").roles("CASHIER"))
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isOk());
        confirm(session, token(result)).andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @Test
    void formSaiHuyTokenCu() throws Exception {
        var session = new MockHttpSession();
        var result = submitPreview(session).andReturn();
        mvc.perform(post("/shifts/close").session(session).with(user("cashier").roles("CASHIER"))
                        .with(csrf()).param("actualCash", "-1")
                        .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"))
                .andExpect(status().isBadRequest());
        confirm(session, token(result)).andExpect(status().isBadRequest());
        verify(closing, never()).close(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shifts/close", "/shifts/close/confirm"})
    void thieuCsrfBiTuChoi(String path) throws Exception {
        mvc.perform(post(path).with(user("cashier").roles("CASHIER"))).andExpect(status().isForbidden());
        verifyNoInteractions(closing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void vaiTroKhacKhongDuocDongCa(String role) throws Exception {
        when(accounts.findByUsername("other")).thenReturn(Optional.of(new Account(
                2L, "other", "unused", "Khác", role, "ACTIVE", Set.of())));
        mvc.perform(get("/shifts/close").with(user("other").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/shifts/close/confirm").with(user("other").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(closing);
    }

    @Test
    void chuaDangNhapPhaiDangNhap() throws Exception {
        mvc.perform(get("/shifts/close")).andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(closing);
    }

    @Test
    void khongCoCaMoKhongHienXacNhan() throws Exception {
        when(closing.preview(eq("cashier"), any())).thenThrow(new BusinessException("Không có ca đang mở"));
        submitPreview(new MockHttpSession()).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Không có ca đang mở")));
        verify(closing, never()).close(any(), any());
    }

    private ResultActions submitPreview(MockHttpSession session) throws Exception {
        return mvc.perform(post("/shifts/close").session(session)
                .with(user("cashier").roles("CASHIER")).with(csrf()).param("actualCash", "560000")
                .flashAttr("uiDisplayName", "Thu ngân").flashAttr("uiInitials", "TN"));
    }

    private ResultActions confirm(MockHttpSession session, String token) throws Exception {
        return mvc.perform(post("/shifts/close/confirm").session(session)
                .with(user("cashier").roles("CASHIER")).with(csrf())
                .param("confirmationToken", token).param("confirmed", "true")
                .param("actualCash", "9999999").param("expectedCash", "0").param("shiftId", "999"));
    }

    private String token(MvcResult result) {
        return (String) result.getModelAndView().getModel().get("confirmationToken");
    }
}
