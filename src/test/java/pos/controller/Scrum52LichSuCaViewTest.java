package pos.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
import pos.dto.ShiftHistory;
import pos.dto.ShiftInvoice;
import pos.entity.Account;
import pos.exception.BusinessException;
import pos.repository.AccountRepository;
import pos.service.CustomUserDetailsService;
import pos.service.ShiftHistoryService;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShiftHistoryController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = UiViewAdvice.class))
@Import({SecurityConfig.class, CustomUserDetailsService.class, PosTime.class})
class Scrum52LichSuCaViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountRepository accounts;
    @MockitoBean private ShiftHistoryService history;
    private final ShiftHistory.Options options = new ShiftHistory.Options(
            List.of(new ShiftHistory.Choice(10, "Thu ngân cũ")), List.of(new ShiftHistory.Choice(1, "Quầy 1")));

    @BeforeEach
    void setup() {
        for (String role : List.of("MANAGER", "ADMIN", "CASHIER")) {
            when(accounts.findByUsername(role)).thenReturn(Optional.of(
                    new Account(1L, role, "unused", "Người dùng", role, "ACTIVE", Set.of())));
        }
        when(history.options(anyString())).thenReturn(options);
        when(history.search(anyString(), any(), anyInt())).thenReturn(new ShiftHistory.SearchResult(
                new ShiftHistory.Page<>(List.of(), 0, false), options));
        when(history.detail(anyString(), eq(30L), anyInt())).thenReturn(new ShiftHistory.DetailResult(
                detail("CLOSED"), new ShiftHistory.Page<>(List.of(), 0, false)));
    }

    private MockHttpServletRequestBuilder request(String path, String role) {
        return get(path).with(user(role).roles(role)).flashAttr("uiDisplayName", "Người dùng").flashAttr("uiInitials", "ND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void ketQuaRongLaTrangThanhCong(String role) throws Exception {
        mvc.perform(request("/shifts/history", role)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có ca phù hợp")))
                .andExpect(content().string(containsString("Thu ngân cũ")))
                .andExpect(content().string(containsString("Lịch sử ca")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shifts/history", "/shifts/history/30"})
    void thuNganBiCamKeCaGuiBoLocGia(String path) throws Exception {
        mvc.perform(request(path, "CASHIER").param("role", "ADMIN").param("cashierId", "10"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(history);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shifts/history", "/shifts/history/30"})
    void chuaDangNhapPhaiDangNhap(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(history);
    }

    @ParameterizedTest
    @CsvSource({"fromDate,abc", "toDate,2026-02-30", "cashierId,0", "cashierId,abc", "registerId,-1", "status,INVALID"})
    void boLocSaiHienThiLoiKhongPhaiTrangRong(String field, String value) throws Exception {
        mvc.perform(request("/shifts/history", "MANAGER").param(field, value)).andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("filter", field))
                .andExpect(content().string(not(containsString("Không có ca phù hợp"))));
        verify(history, never()).search(anyString(), any(), anyInt());
    }

    @Test
    void loiKhoangNgayDuocHienThiTrongTrang() throws Exception {
        when(history.search(anyString(), any(), anyInt())).thenThrow(new BusinessException("Ngày bắt đầu không được sau ngày kết thúc."));
        mvc.perform(request("/shifts/history", "MANAGER").param("fromDate", "2026-11-26").param("toDate", "2026-11-25"))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("Ngày bắt đầu không được sau ngày kết thúc.")));
    }

    @Test
    void bangHienThiDuThongTinVaPhanTrangGiuBoLoc() throws Exception {
        when(history.search(anyString(), any(), eq(1))).thenReturn(new ShiftHistory.SearchResult(
                new ShiftHistory.Page<>(List.of(detail("CLOSED").shift()), 1, true), options));
        mvc.perform(request("/shifts/history", "MANAGER").param("page", "1").param("status", "CLOSED")
                        .param("cashierId", "10").param("registerId", "1").param("fromDate", "2026-11-25"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("CA-30")))
                .andExpect(content().string(containsString("25/11/2026 07:00:00")))
                .andExpect(content().string(containsString("Đã kết thúc")))
                .andExpect(content().string(containsString("/shifts/history/30")))
                .andExpect(content().string(containsString("page=2")))
                .andExpect(content().string(containsString("cashierId=10")))
                .andExpect(content().string(containsString("status=CLOSED")));
        verify(history).search(eq("MANAGER"), argThat(f -> f.getCashierId() == 10L && f.getRegisterId() == 1L), eq(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void chiTietHienThiSoDaChotVaChenhLechAm(String role) throws Exception {
        mvc.perform(request("/shifts/history/30", role)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Kết quả bàn giao đã lưu")))
                .andExpect(content().string(containsString("50.000 VNĐ")))
                .andExpect(content().string(containsString("6.000 VNĐ")))
                .andExpect(content().string(containsString("56.000 VNĐ")))
                .andExpect(content().string(containsString("55.000 VNĐ")))
                .andExpect(content().string(containsString("-1.000 VNĐ")))
                .andExpect(content().string(containsString("Ca chưa có hóa đơn hoàn tất.")));
    }

    @Test
    void caMoKhongHienThiSoChotGiaLap() throws Exception {
        when(history.detail("MANAGER", 30, 0)).thenReturn(new ShiftHistory.DetailResult(detail("OPEN"), new ShiftHistory.Page<>(List.of(), 0, false)));
        mvc.perform(request("/shifts/history/30", "MANAGER")).andExpect(status().isOk())
                .andExpect(content().string(containsString("chưa có kết quả đối soát đã chốt")))
                .andExpect(content().string(not(containsString("Kết quả bàn giao đã lưu"))));
    }

    @Test
    void hoaDonDungLienKetVaPhanTrangTheoCa() throws Exception {
        var invoice = new ShiftInvoice(81L, "HD-81", "Thu ngân", time(), new BigDecimal("6000"), "CASH");
        when(history.detail("ADMIN", 30, 1)).thenReturn(new ShiftHistory.DetailResult(detail("CLOSED"), new ShiftHistory.Page<>(List.of(invoice), 1, true)));
        mvc.perform(request("/shifts/history/30", "ADMIN").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/invoices/81")))
                .andExpect(content().string(containsString("HD-81")))
                .andExpect(content().string(containsString("/shifts/history/30?page=2")))
                .andExpect(content().string(not(containsString("Ca chưa có hóa đơn hoàn tất."))));
        verify(history).detail("ADMIN", 30, 1);
    }

    @Test
    void trangKhongPhaiSoBiTuChoi() throws Exception {
        mvc.perform(request("/shifts/history", "MANAGER").param("page", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(history);
    }

    private Timestamp time() {
        return Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"));
    }

    private ShiftHistory.Detail detail(String status) {
        var row = new ShiftHistory.Row(30, "CA-30", status, 10, "Thu ngân", "NV10", 1, "Quầy 1", time(), status.equals("CLOSED") ? time() : null);
        return new ShiftHistory.Detail(row, new BigDecimal("50000"), status.equals("OPEN") ? null : new BigDecimal("6000"),
                new BigDecimal("6000"), BigDecimal.ZERO, new BigDecimal("56000"), new BigDecimal("55000"), new BigDecimal("-1000"));
    }
}
