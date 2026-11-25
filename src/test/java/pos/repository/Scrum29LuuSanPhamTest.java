package pos.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import pos.entity.InventoryMovementEntity;
import pos.entity.ProductEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum29LuuSanPhamTest {
    private EntityManager em;
    private PosRepository db;
    private ProductEntity product;
    private Timestamp originalTime;

    @BeforeEach
    void setup() {
        em = mock(EntityManager.class);
        db = new PosRepository(em);
        product = new ProductEntity();
        product.setId(20L);
        product.setCode("SP20");
        product.setName("Tên cũ");
        product.setBarcode("001234");
        product.setStockQuantity(new BigDecimal("10"));
        product.setReservedQuantity(new BigDecimal("2"));
        product.setStatus("STOPPED");
        originalTime = Timestamp.from(Instant.parse("2026-11-25T00:00:00Z"));
        product.setCreatedAt(originalTime);
        product.setStoppedAt(originalTime);
        when(em.find(ProductEntity.class, 20L)).thenReturn(product);
    }

    @ParameterizedTest
    @CsvSource({"15,5", "5,-5"})
    void doiTonKhoGhiDungChenhLechVaGiuCacCotKhac(String stock, String delta) {
        var now = Timestamp.from(Instant.parse("2026-11-25T01:00:00Z"));
        db.updateProduct(20, "000042", "Tên mới", 2, 1, new BigDecimal("25000"), new BigDecimal(stock),
                "https://example.com/a.png", now);
        assertEquals("000042", product.getBarcode());
        assertEquals("Tên mới", product.getName());
        assertEquals(2L, product.getCategoryId());
        assertEquals(1L, product.getUnitId());
        assertEquals(new BigDecimal("25000"), product.getSalePrice());
        assertEquals(new BigDecimal(stock), product.getStockQuantity());
        assertEquals(new BigDecimal("2"), product.getReservedQuantity());
        assertEquals("SP20", product.getCode());
        assertEquals("STOPPED", product.getStatus());
        assertEquals(originalTime, product.getCreatedAt());
        assertEquals(originalTime, product.getStoppedAt());
        assertEquals(now, product.getUpdatedAt());
        var movement = ArgumentCaptor.forClass(InventoryMovementEntity.class);
        verify(em).persist(movement.capture());
        assertEquals(new BigDecimal(delta), movement.getValue().getQuantityDelta());
        assertEquals("ADJUSTMENT", movement.getValue().getMovementType());
        assertEquals("SP20", movement.getValue().getProductCodeSnapshot());
        assertEquals(20L, movement.getValue().getProductId());
        assertTrue(movement.getValue().getEventKey().startsWith("PRODUCT-EDIT-"));
        verify(em).flush();
    }

    @Test
    void khongDoiTonKhoKhongTaoChuyenDongKho() {
        db.updateProduct(20, "001234", "Tên mới", 2, 1, new BigDecimal("25000"), new BigDecimal("10.000"), null, originalTime);
        verify(em, never()).persist(any());
        assertNull(product.getImageUrl());
        verify(em).flush();
    }
}
