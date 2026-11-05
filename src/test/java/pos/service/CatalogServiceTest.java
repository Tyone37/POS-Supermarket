package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class CatalogServiceTest {
    private PosRepository db;
    private CatalogService service;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new CatalogService(db);
        when(db.account("manager", true))
                .thenReturn(
                        Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "MANAGER")));
        when(db.units()).thenReturn(List.of(Map.of("id", 1L, "code", "CAI", "quantity_scale", 0)));
        when(db.categories()).thenReturn(List.of(Map.of("id", 2L, "code", "UNCLASSIFIED")));
    }

    @Test
    void themSanPhamHopLe() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "10", null);
        when(db.createProduct(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any(),
                        any(),
                        isNull(),
                        any()))
                .thenReturn(20L);
        assertEquals(20L, service.create("manager", form));
    }

    @Test
    void themSanPhamGiaAm() {
        Forms.Product form = new Forms.Product(null, "893001", "Sữa", null, null, "-1", "10", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    @Test
    void themSanPhamTonKhoAm() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "-1", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    @Test
    void timSanPhamTheoTenKhongDau() {
        Map<String, Object> product =
                Map.of("code", "SP001", "name", "Sữa tươi", "status", "ACTIVE");
        when(db.products(null)).thenReturn(List.of(product));
        assertEquals(List.of(product), service.search("sua", null, true));
    }
}
