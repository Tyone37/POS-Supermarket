package pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SCRUM-28 — test riêng theo user story. */
class Scrum28ThemSanPhamTest {
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

    // Thêm sản phẩm với dữ liệu hợp lệ.
    @Test
    void themSanPhamHopLe() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "10", null);
        when(db.createProduct(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any(),
                        any(),
                        isNull(),
                        any()))
                .thenReturn(20L);
        assertEquals(20L, service.create("manager", form));
    }

    // Từ chối giá bán âm.
    @Test
    void themSanPhamGiaAm() {
        Forms.Product form = new Forms.Product(null, "893001", "Sữa", null, null, "-1", "10", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối tồn kho âm.
    @Test
    void themSanPhamTonKhoAm() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "-1", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối tên sản phẩm trống.
    @Test
    void themSanPhamBoTrongTen() {
        Forms.Product form = new Forms.Product(null, "893001", "", null, null, "20000", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Tên sản phẩm"));
    }

    // Từ chối mã vạch trống.
    @Test
    void themSanPhamBoTrongMaVach() {
        Forms.Product form = new Forms.Product(null, "", "Sữa", null, null, "20000", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Mã vạch"));
    }

    // Từ chối giá bán chứa chữ.
    @Test
    void themSanPhamNhapChuVaoGia() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "abc", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Đơn giá"));
    }

    // Từ chối giá bán bằng không.
    @Test
    void themSanPhamGiaBangKhong() {
        Forms.Product form = new Forms.Product(null, "893001", "Sữa", null, null, "0", "10", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối tồn kho chứa chữ.
    @Test
    void themSanPhamNhapChuVaoTonKho() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "abc", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Tồn kho"));
    }

    // Từ chối mã vạch bị trùng.
    @Test
    void themSanPhamMaVachTrung() {
        when(db.products(null)).thenReturn(List.of(Map.of("code", "SP001", "barcode", "893001")));
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("đã tồn tại"));
    }

    // Từ chối giá bán trống.
    @Test
    void themSanPhamGiaBoTrong() {
        Forms.Product form = new Forms.Product(null, "893001", "Sữa", null, null, "", "10", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối giá bán vượt giới hạn.
    @Test
    void themSanPhamGiaVuotGioiHan() {
        Forms.Product form =
                new Forms.Product(
                        null, "893001", "Sữa", null, null, "10000000000000000", "10", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối tồn kho trống.
    @Test
    void themSanPhamTonKhoBoTrong() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối tồn kho vượt giới hạn.
    @Test
    void themSanPhamTonKhoVuotGioiHan() {
        Forms.Product form =
                new Forms.Product(
                        null, "893001", "Sữa", null, null, "20000", "1000000000000", null);
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối nhóm hàng không hợp lệ.
    @Test
    void themSanPhamNhomHangKhongHopLe() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", 999L, null, "20000", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Nhóm hàng"));
    }

    // Từ chối đơn vị không hợp lệ.
    @Test
    void themSanPhamDonViKhongHopLe() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, 999L, "20000", "10", null);
        BusinessException error =
                assertThrows(BusinessException.class, () -> service.create("manager", form));
        assertTrue(error.getMessage().contains("Đơn vị"));
    }

    // Từ chối ảnh không dùng HTTPS.
    @Test
    void themSanPhamAnhKhongPhaiHttps() {
        Forms.Product form =
                new Forms.Product(
                        null,
                        "893001",
                        "Sữa",
                        null,
                        null,
                        "20000",
                        "10",
                        "http://example.com/a.png");
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }

    // Từ chối địa chỉ ảnh không hợp lệ.
    @Test
    void themSanPhamAnhKhongPhaiUrl() {
        Forms.Product form =
                new Forms.Product(null, "893001", "Sữa", null, null, "20000", "10", "anh-san-pham");
        assertThrows(BusinessException.class, () -> service.create("manager", form));
    }
}
