package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SCRUM-36 — test riêng theo user story. */
class Scrum36TimSanPhamTest {
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
    void timSanPhamTheoTenKhongDau() {
        Map<String, Object> product =
                Map.of("code", "SP001", "name", "Sữa tươi", "status", "ACTIVE");
        when(db.products(null)).thenReturn(List.of(product));
        assertEquals(List.of(product), service.search("sua", null, true));
    }

    @Test
    void timSanPhamBangSku() {
        Map<String, Object> product = Map.of("code", "SP001", "name", "Sữa", "status", "ACTIVE");
        when(db.products(null)).thenReturn(List.of(product));
        assertEquals(List.of(product), service.search("sp001", null, true));
    }

    @Test
    void timSanPhamKhongCoKetQua() {
        when(db.products(null))
                .thenReturn(List.of(Map.of("code", "SP001", "name", "Sữa", "status", "ACTIVE")));
        assertTrue(service.search("banh", null, true).isEmpty());
    }

    @Test
    void timSanPhamTheoNhomHang() {
        Map<String, Object> product = Map.of("code", "SP001", "name", "Sữa", "status", "ACTIVE");
        when(db.products(2L)).thenReturn(List.of(product));
        assertEquals(List.of(product), service.search("", 2L, true));
        verify(db).products(2L);
    }
}
