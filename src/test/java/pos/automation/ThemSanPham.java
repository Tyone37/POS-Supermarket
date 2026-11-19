package pos.automation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pos.config.TransferSettings;
import pos.controller.PosController;
import pos.dto.Forms;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.SaleService;
import pos.service.ShiftService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ThemSanPham {
    private MockMvc mvc;
    private CatalogService catalog;

    @BeforeEach
    void setup() {
        catalog = mock(CatalogService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PosController(
                catalog,
                mock(AccountAdminService.class),
                mock(ShiftService.class),
                mock(SaleService.class),
                new TransferSettings("970422", "012311234", "NGUYEN THANH TOAN")))
                .build();
    }

    @Test
    void guiFormTaoSanPhamThanhCong() throws Exception {
        when(catalog.create(eq("manager"), any(Forms.Product.class))).thenReturn(41L);

        mvc.perform(post("/products")
                        .principal(() -> "manager")
                        .param("barcode", "AUTO-89310001")
                        .param("name", "Sữa kiểm thử")
                        .param("price", "20000")
                        .param("stock", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attribute("message",
                        "Đã thêm sản phẩm. Thu ngân có thể quét ngay tại quầy."));

        var form = org.mockito.ArgumentCaptor.forClass(Forms.Product.class);
        verify(catalog).create(eq("manager"), form.capture());
        assertEquals("AUTO-89310001", form.getValue().barcode());
        assertEquals("Sữa kiểm thử", form.getValue().name());
    }
}
