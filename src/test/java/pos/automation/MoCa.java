package pos.automation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pos.config.TransferSettings;
import pos.controller.PosController;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.SaleService;
import pos.service.ShiftService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoCa {
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
    void guiMoCaVoiTienDauCa() throws Exception {
        when(shifts.open(eq("cashier"), eq(20L), eq("500000"), anyString()))
                .thenReturn(30L);

        mvc.perform(post("/shifts")
                        .principal(() -> "cashier")
                        .param("registerId", "20")
                        .param("openingCash", "500000")
                        .param("openRequestKey", "open-key-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales"))
                .andExpect(flash().attribute("message",
                        "Ca đang mở. Có thể tiếp tục bán hàng."));

        verify(shifts).open("cashier", 20L, "500000", "open-key-123");
    }
}
