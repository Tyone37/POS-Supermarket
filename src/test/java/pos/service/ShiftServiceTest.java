package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class ShiftServiceTest {
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
    void moCaVoiTienHopLe() {
        when(db.createShift(anyString(), anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(30L);
        long id = service.open("cashier", 20L, "500000", "open-key-123");
        assertEquals(30L, id);
    }

    @Test
    void moCaVoiTienAm() {
        assertThrows(
                BusinessException.class, () -> service.open("cashier", 20L, "-1", "open-key-123"));
    }

    @Test
    void moCaChuaChonQuay() {
        assertThrows(
                BusinessException.class,
                () -> service.open("cashier", null, "500000", "open-key-123"));
    }

    @Test
    void xemKhiChuaCoCa() {
        assertTrue(service.current("cashier").isEmpty());
    }
}
