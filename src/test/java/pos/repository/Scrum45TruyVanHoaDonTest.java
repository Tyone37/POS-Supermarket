package pos.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import pos.dto.InvoiceSearch;
import pos.entity.InvoiceEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Scrum45TruyVanHoaDonTest {
    private EntityManager em;
    private TypedQuery<Object[]> query;
    private InvoiceSearchRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        em = mock(EntityManager.class);
        query = mock(TypedQuery.class, RETURNS_SELF);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        repository = new InvoiceSearchRepository(em);
    }

    @Test
    void ketHopDieuKienVaGioiHanChuHoaDonTaiDatabase() {
        repository.search(new InvoiceSearch.Query("HD-1", "CASH", "VOID", 7L, 40), 21);
        var jpql = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(jpql.capture(), eq(Object[].class));
        String sql = jpql.getValue();
        assertTrue(sql.contains("i.paymentId = p.id and i.orderId = p.orderId"));
        assertTrue(sql.contains("and i.issuedBy = :issuer"));
        assertTrue(sql.contains("and lower(i.invoiceNo) like :code escape '!'"));
        assertTrue(sql.contains("and p.method = :method"));
        assertTrue(sql.contains("and i.status = :status"));
        assertTrue(sql.endsWith("order by i.issuedAt desc, i.id desc"));
        verify(query).setParameter("issuer", 7L);
        verify(query).setParameter("code", "%hd-1%");
        verify(query).setParameter("method", "CASH");
        verify(query).setParameter("status", "VOID");
        verify(query).setFirstResult(40);
        verify(query).setMaxResults(21);
        verify(em, never()).flush();
    }

    @Test
    void quanLyKhongBiGioiHanTheoNguoiLapHoacTrangThaiMacDinh() {
        repository.search(new InvoiceSearch.Query("", "", "", null, 0), 21);
        var jpql = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(jpql.capture(), eq(Object[].class));
        assertFalse(jpql.getValue().contains("i.issuedBy ="));
        assertFalse(jpql.getValue().contains("i.status ="));
        assertFalse(jpql.getValue().contains("p.method ="));
        assertFalse(jpql.getValue().contains("like"));
        verify(query, never()).setParameter(anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @ParameterizedTest
    @CsvSource({"HD%1,%hd!%1%", "HD_1,%hd!_1%", "HD!1,%hd!!1%", "HD' OR 1=1,%hd' or 1=1%"})
    void maDuocBindVaEscapeKyTuDacBiet(String input, String expected) {
        repository.search(new InvoiceSearch.Query(input, "", "", 7L, 0), 21);
        verify(query).setParameter("code", expected);
        var jpql = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(jpql.capture(), eq(Object[].class));
        assertFalse(jpql.getValue().contains(input));
    }

    @Test
    void anhXaSnapshotVaMaLienKetKhongSuaEntity() {
        var invoice = new InvoiceEntity();
        invoice.setId(81L);
        invoice.setInvoiceNo("HD-81");
        invoice.setIssuedAt(Timestamp.from(Instant.parse("2026-11-25T00:00:00Z")));
        invoice.setCashierNameSnapshot("Tên lúc bán");
        invoice.setTotalAmount(new BigDecimal("12345.67"));
        invoice.setStatus("ISSUED");
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{invoice, "BANK_TRANSFER"}));
        var row = repository.search(new InvoiceSearch.Query("", "", "", 7L, 0), 21).getFirst();
        assertEquals(81L, row.id());
        assertEquals("HD-81", row.invoiceNo());
        assertEquals(invoice.getIssuedAt(), row.issuedAt());
        assertEquals("Tên lúc bán", row.cashierName());
        assertEquals("BANK_TRANSFER", row.method());
        assertEquals("ISSUED", row.status());
        assertEquals(new BigDecimal("12345.67"), row.totalAmount());
        assertEquals("ISSUED", invoice.getStatus());
        verify(em, never()).persist(invoice);
        verify(em, never()).merge(invoice);
        verify(em, never()).remove(invoice);
    }
}
