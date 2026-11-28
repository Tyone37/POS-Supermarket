package pos.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Scrum51BatBienCaTest {
    private ShiftEntity shift;
    private Timestamp closedAt;

    @BeforeEach
    void setup() {
        shift = new ShiftEntity();
        shift.setStatus("OPEN");
        shift.setOpeningCash(new BigDecimal("500000.00"));
        closedAt = Timestamp.from(Instant.parse("2026-11-25T10:00:00Z"));
        shift.close(closedAt, new BigDecimal("120000.00"), new BigDecimal("100000.00"),
                new BigDecimal("20000.00"), new BigDecimal("580000.00"),
                new BigDecimal("570000.00"), new BigDecimal("-10000.00"));
    }

    @Test
    void dongCaLanHaiKhongGhiDeBatKySoLieuNao() {
        var saved = figures();

        assertThrows(IllegalStateException.class, () -> shift.close(
                Timestamp.from(Instant.parse("2026-11-26T10:00:00Z")), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        assertEquals(saved, figures());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED"})
    void khongDoiTrangThaiCaDaDong(String status) {
        var saved = figures();
        assertThrows(IllegalStateException.class, () -> shift.setStatus(status));
        assertEquals(saved, figures());
    }

    @Test
    void khongSuaTienDauCaSauKetThuc() {
        var saved = figures();
        assertThrows(IllegalStateException.class, () -> shift.setOpeningCash(BigDecimal.ZERO));
        assertEquals(saved, figures());
    }

    @Test
    void khongSuaGioKetThucSauChot() {
        var saved = figures();
        assertThrows(IllegalStateException.class, () -> shift.setClosedAt(null));
        assertEquals(saved, figures());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "CLOSED"})
    void chiDongCaDangMo(String status) {
        var other = new ShiftEntity();
        other.setStatus(status);
        assertThrows(IllegalStateException.class, () -> other.close(closedAt, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private List<Object> figures() {
        return List.of(shift.getStatus(), shift.getOpeningCash(), shift.getClosedAt(), shift.getClosingRevenue(),
                shift.getClosingCashSales(), shift.getClosingCashRefunds(), shift.getClosingExpectedCash(),
                shift.getClosingActualCash(), shift.getClosingDifference());
    }
}
