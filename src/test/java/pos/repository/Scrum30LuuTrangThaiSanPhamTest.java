package pos.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pos.entity.ProductEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class Scrum30LuuTrangThaiSanPhamTest {
    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ACTIVE"})
    void chiDoiTrangThaiVaThoiDiemKhongSuaThongTinBanHang(String status) {
        var em = mock(EntityManager.class);
        var product = product();
        var before = product.getCreatedAt();
        var now = Timestamp.from(Instant.parse("2026-11-26T01:00:00Z"));
        when(em.find(ProductEntity.class, 20L)).thenReturn(product);
        new PosRepository(em).changeProductStatus(20, status, now);
        assertEquals(status, product.getStatus());
        assertEquals(status.equals("STOPPED") ? now : null, product.getStoppedAt());
        assertEquals(now, product.getUpdatedAt());
        assertEquals(before, product.getCreatedAt());
        assertEquals("SP20", product.getCode());
        assertEquals("001234", product.getBarcode());
        assertEquals("Tên gốc", product.getName());
        assertEquals(new BigDecimal("20000"), product.getSalePrice());
        assertEquals(new BigDecimal("10"), product.getStockQuantity());
        assertEquals(new BigDecimal("2"), product.getReservedQuantity());
        assertEquals(1L, product.getUnitId());
        assertEquals(2L, product.getCategoryId());
        verify(em).find(ProductEntity.class, 20L);
        verify(em).flush();
        verifyNoMoreInteractions(em);
    }

    @Test
    void kichHoatLaiXoaStoppedAtCuDeDungRangBuocSchema() {
        var em = mock(EntityManager.class);
        var product = product();
        product.setStatus("STOPPED");
        product.setStoppedAt(product.getCreatedAt());
        when(em.find(ProductEntity.class, 20L)).thenReturn(product);
        new PosRepository(em).changeProductStatus(20, "ACTIVE", Timestamp.from(Instant.now()));
        assertNull(product.getStoppedAt());
        assertEquals("ACTIVE", product.getStatus());
    }

    private ProductEntity product() {
        var product = new ProductEntity();
        product.setId(20L);
        product.setCode("SP20");
        product.setBarcode("001234");
        product.setName("Tên gốc");
        product.setUnitId(1L);
        product.setCategoryId(2L);
        product.setSalePrice(new BigDecimal("20000"));
        product.setStockQuantity(new BigDecimal("10"));
        product.setReservedQuantity(new BigDecimal("2"));
        product.setCreatedAt(Timestamp.from(Instant.parse("2026-11-25T00:00:00Z")));
        return product;
    }
}
