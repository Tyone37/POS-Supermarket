package pos.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pos.dto.ShiftClosePreview;
import pos.entity.ShiftEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Unit test, no database or Spring context. */
class Scrum50LuuKetQuaCaTest {
    @ParameterizedTest
    @CsvSource({"590000,-10000", "600000,0", "610000,10000", "0,-600000"})
    void luuDayDuVaKhongSuaCaGoc(String actual, String difference) {
        var em = mock(EntityManager.class);
        var repository = new PosRepository(em);
        var openedAt = Timestamp.from(Instant.parse("2026-11-25T01:00:00Z"));
        var closedAt = Timestamp.from(Instant.parse("2026-11-25T10:30:00Z"));
        var shift = new ShiftEntity();
        shift.setId(30L);
        shift.setEmployeeId(10L);
        shift.setRegisterId(20L);
        shift.setOpenedAt(openedAt);
        shift.setOpeningCash(new BigDecimal("500000.00"));
        shift.setStatus("OPEN");
        when(em.find(ShiftEntity.class, 30L)).thenReturn(shift);
        var figures = new ShiftClosePreview(30L, "CA-30", openedAt, shift.getOpeningCash(), 2,
                new BigDecimal("150000.00"), new BigDecimal("120000.00"), new BigDecimal("20000.00"),
                new BigDecimal("600000.00"), new BigDecimal(actual), new BigDecimal(difference));

        repository.closeShift(figures, closedAt);

        assertEquals("CLOSED", shift.getStatus());
        assertEquals(closedAt, shift.getClosedAt());
        assertEquals(figures.revenue(), shift.getClosingRevenue());
        assertEquals(figures.cashSales(), shift.getClosingCashSales());
        assertEquals(figures.cashRefunds(), shift.getClosingCashRefunds());
        assertEquals(figures.expectedCash(), shift.getClosingExpectedCash());
        assertEquals(figures.actualCash(), shift.getClosingActualCash());
        assertEquals(figures.difference(), shift.getClosingDifference());
        assertEquals(openedAt, shift.getOpenedAt());
        assertEquals(new BigDecimal("500000.00"), shift.getOpeningCash());
        assertEquals(10L, shift.getEmployeeId());
        assertEquals(20L, shift.getRegisterId());
        verify(em).find(ShiftEntity.class, 30L);
        verify(em).flush();
        verifyNoMoreInteractions(em);
    }
}
