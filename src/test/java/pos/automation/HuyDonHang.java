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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HuyDonHang {
    private MockMvc mvc;
    private SaleService sales;

    @BeforeEach
    void setup() {
        sales = mock(SaleService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PosController(
                mock(CatalogService.class),
                mock(AccountAdminService.class),
                mock(ShiftService.class),
                sales,
                new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN")))
                .build();
    }

    @Test
    void guiYeuCauHuyDonKemLyDo() throws Exception {
        mvc.perform(post("/sales/10/cancel")
                        .principal(() -> "cashier")
                        .param("reason", "Khách đổi ý")
                        .param("confirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/10"));

        verify(sales).cancel("cashier", 10L, "Khách đổi ý", true);
    }
}
