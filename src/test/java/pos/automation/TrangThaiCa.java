package pos.automation;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pos.config.TransferSettings;
import pos.dto.ShiftSummary;
import pos.controller.PosController;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.SaleService;
import pos.service.ShiftService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class TrangThaiCa {
    private MockMvc mvc;
    private ShiftService shifts;

    @BeforeEach
    void setup() {
        shifts = mock(ShiftService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PosController(
                mock(CatalogService.class),
                mock(AccountAdminService.class),
                shifts,
                mock(SaleService.class),
                new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN")))
                .build();
    }

    @Test
    void xemTrangThaiCaDangMo() throws Exception {
        Map<String, Object> currentShift = Map.of("id", 30L, "status", "OPEN");
        when(shifts.summary("cashier")).thenReturn(
                new ShiftSummary(currentShift, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));
        when(shifts.registers()).thenReturn(List.of());

        mvc.perform(get("/shifts").principal(() -> "cashier"))
                .andExpect(status().isOk())
                .andExpect(view().name("pos/shifts"))
                .andExpect(model().attribute("shift", currentShift));
        verify(shifts).summary("cashier");
    }
}
