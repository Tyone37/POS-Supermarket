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

class DieuChinhSoLuong {
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
    void guiCapNhatSoLuongTrongDon() throws Exception {
        mvc.perform(post("/sales/10/quantity")
                        .principal(() -> "cashier")
                        .param("itemId", "30")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/10"));

        verify(sales).quantity("cashier", 10L, 30L, "2", false);
    }

    @Test
    void guiXacNhanXoaDongHang() throws Exception {
        mvc.perform(post("/sales/10/remove")
                        .principal(() -> "cashier")
                        .param("itemId", "30")
                        .param("confirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/10"));

        verify(sales).quantity("cashier", 10L, 30L, "0", true);
    }
}
