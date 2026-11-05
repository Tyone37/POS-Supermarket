package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class AccountAdminServiceTest {
    private PosRepository db;
    private AccountAdminService service;

    @BeforeEach
    void setup() {
        db = mock(PosRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        service = new AccountAdminService(db, encoder);
        when(db.account("admin", true))
                .thenReturn(
                        Optional.of(Map.of("id", 1L, "status", "ACTIVE", "role_code", "ADMIN")));
        when(db.roles()).thenReturn(List.of(Map.of("id", 2L, "code", "CASHIER")));
        when(encoder.encode("password123")).thenReturn("mat-khau-da-bam");
    }

    @Test
    void taoTaiKhoanHopLe() {
        Forms.NewAccount form =
                new Forms.NewAccount("cashier2", "password123", "Nguyễn An", "CASHIER");
        when(db.createAccount(anyString(), anyString(), anyString(), anyLong(), any()))
                .thenReturn(10L);
        long id = service.create("admin", form);
        assertEquals(10L, id);
        verify(db)
                .createAccount(
                        eq("cashier2"), eq("mat-khau-da-bam"), eq("Nguyễn An"), eq(2L), any());
    }

    @Test
    void taoTaiKhoanBiTrungTen() {
        when(db.account("cashier2", false)).thenReturn(Optional.of(Map.of("id", 10L)));
        Forms.NewAccount form =
                new Forms.NewAccount("cashier2", "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    @Test
    void taoTaiKhoanMatKhauQuaNgan() {
        Forms.NewAccount form = new Forms.NewAccount("cashier2", "123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    @Test
    void khongDuocTuKhoaTaiKhoan() {
        assertThrows(BusinessException.class, () -> service.lock("admin", 1L, true));
        verify(db, never()).lockAccount(anyLong(), any());
    }
}
