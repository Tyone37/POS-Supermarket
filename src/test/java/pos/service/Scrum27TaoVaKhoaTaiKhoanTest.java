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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SCRUM-27 — test riêng theo user story. */
class Scrum27TaoVaKhoaTaiKhoanTest {
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

    // Tạo tài khoản với dữ liệu hợp lệ.
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

    // Từ chối tên tài khoản bị trùng.
    @Test
    void taoTaiKhoanBiTrungTen() {
        when(db.account("cashier2", false)).thenReturn(Optional.of(Map.of("id", 10L)));
        Forms.NewAccount form =
                new Forms.NewAccount("cashier2", "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối mật khẩu quá ngắn.
    @Test
    void taoTaiKhoanMatKhauQuaNgan() {
        Forms.NewAccount form = new Forms.NewAccount("cashier2", "123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Không cho phép tự khóa tài khoản.
    @Test
    void khongDuocTuKhoaTaiKhoan() {
        assertThrows(BusinessException.class, () -> service.lock("admin", 1L, true));
        verify(db, never()).lockAccount(anyLong(), any());
    }

    // Từ chối ký tự không hợp lệ trong tên tài khoản.
    @Test
    void taoTaiKhoanCoKyTuKhongHopLe() {
        Forms.NewAccount form =
                new Forms.NewAccount("user@name", "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối họ tên trống.
    @Test
    void taoTaiKhoanBoTrongHoTen() {
        Forms.NewAccount form = new Forms.NewAccount("cashier2", "password123", "", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối tên đăng nhập trống.
    @Test
    void taoTaiKhoanBoTrongUsername() {
        Forms.NewAccount form = new Forms.NewAccount("", "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối tên đăng nhập quá ngắn.
    @Test
    void taoTaiKhoanUsernameQuaNgan() {
        Forms.NewAccount form = new Forms.NewAccount("ab", "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối tên đăng nhập quá dài.
    @Test
    void taoTaiKhoanUsernameQuaDai() {
        Forms.NewAccount form =
                new Forms.NewAccount("a".repeat(101), "password123", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối mật khẩu trống.
    @Test
    void taoTaiKhoanBoTrongMatKhau() {
        Forms.NewAccount form = new Forms.NewAccount("cashier2", "", "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Từ chối mật khẩu quá dài.
    @Test
    void taoTaiKhoanMatKhauQuaDai() {
        Forms.NewAccount form =
                new Forms.NewAccount("cashier2", "a".repeat(73), "Nguyễn An", "CASHIER");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Yêu cầu chọn vai trò khi tạo tài khoản.
    @Test
    void taoTaiKhoanChuaChonVaiTro() {
        Forms.NewAccount form = new Forms.NewAccount("cashier2", "password123", "Nguyễn An", "");
        assertThrows(BusinessException.class, () -> service.create("admin", form));
    }

    // Chặn thu ngân tạo tài khoản.
    @Test
    void thuNganKhongDuocTaoTaiKhoan() {
        when(db.account("cashier", true))
                .thenReturn(
                        Optional.of(Map.of("id", 3L, "status", "ACTIVE", "role_code", "CASHIER")));
        Forms.NewAccount form =
                new Forms.NewAccount("cashier2", "password123", "Nguyễn An", "CASHIER");
        assertThrows(AccessDeniedException.class, () -> service.create("cashier", form));
        verify(db, never()).createAccount(anyString(), anyString(), anyString(), anyLong(), any());
    }

    // Khóa thành công tài khoản khác.
    @Test
    void khoaTaiKhoanKhacThanhCong() {
        when(db.accountById(2L)).thenReturn(Optional.of(Map.of("id", 2L)));
        service.lock("admin", 2L, true);
        verify(db).lockAccount(eq(2L), any());
    }
}
