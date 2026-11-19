package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SCRUM-47 — test riêng theo user story. */
class Scrum47XemTrangThaiCaTest {
    private PosRepository db;
    private ShiftService service;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        service = new ShiftService(db);
        when(db.account("cashier", true))
                .thenReturn(
                        Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "CASHIER")));
        when(db.employee(1L)).thenReturn(Optional.of(Map.of("id", 10L)));
        when(db.registers()).thenReturn(List.of(Map.of("id", 20L)));
    }

    @Test
    void xemKhiChuaCoCa() {
        assertTrue(service.current("cashier").isEmpty());
    }

    @Test
    void xemCaDangMo() {
        Map<String, Object> shift = Map.of("id", 30L, "status", "OPEN");
        when(db.latestShift(10L)).thenReturn(Optional.of(shift));
        assertEquals(shift, service.current("cashier").orElseThrow());
    }

    @Test
    void xemCaDaDong() {
        Map<String, Object> shift = Map.of("id", 30L, "status", "CLOSED");
        when(db.latestShift(10L)).thenReturn(Optional.of(shift));
        assertEquals(shift, service.current("cashier").orElseThrow());
    }
}
