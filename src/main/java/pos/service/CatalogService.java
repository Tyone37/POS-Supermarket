package pos.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class CatalogService {
    private final PosRepository db;
    public CatalogService(PosRepository db) { this.db = db; }

    public List<Map<String, Object>> search(String query, Long category, boolean selling) {
        return search(query, category, selling, 0).stream().limit(selling ? 20 : 100).toList();
    }

    public List<Map<String, Object>> search(String query, Long category, boolean selling, int page) {
        String term = PosSupport.normalized(query);
        return db.products(category).stream()
                .filter(row -> !selling || "ACTIVE".equals(row.get("status")))
                .filter(row -> PosSupport.normalized(row.get("name").toString()).contains(term)
                        || (selling ? row.get("code").toString().equalsIgnoreCase(query == null ? "" : query.strip())
                            : PosSupport.normalized(row.get("code").toString()).contains(term)
                                || row.get("barcode").toString().contains(term)))
                .skip(selling ? Math.max(0L, (long) page) * 20 : 0)
                .limit(selling ? 21 : 100).toList();
    }

    public List<Map<String, Object>> categories() { return db.categories(); }
    public List<Map<String, Object>> units() { return db.units(); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> stoppedProducts(String query) {
        String term = PosSupport.normalized(query);
        return db.products(null).stream()
                .filter(row -> "STOPPED".equals(row.get("status")))
                .filter(row -> term.isEmpty()
                        || PosSupport.normalized(row.get("name").toString()).contains(term)
                        || PosSupport.normalized(row.get("code").toString()).contains(term)
                        || row.get("barcode").toString().contains(term))
                .limit(100)
                .toList();
    }

    @Transactional
    public void delete(String actor, long productId, boolean confirmed) {
        PosSupport.actor(db, actor, "MANAGER");
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận xóa vĩnh viễn sản phẩm.");
        }
        var product = db.product(productId, true)
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));
        if (!"STOPPED".equals(product.get("status"))) {
            throw new BusinessException("Chỉ được xóa sản phẩm đã ngừng bán.");
        }
        if (db.productHasInvoice(productId)) {
            throw new BusinessException("Không thể xóa sản phẩm đã xuất hiện trên hóa đơn.");
        }
        if (db.productHasOrderItem(productId)) {
            throw new BusinessException("Không thể xóa sản phẩm đang có trong đơn hàng.");
        }
        if (db.productHasCoupons(productId)) {
            throw new BusinessException("Sản phẩm đang được cấu hình trong coupon, không thể xóa.");
        }
        db.deleteProduct(productId);
    }

    @Transactional
    public long create(String actor, Forms.Product form) {
        PosSupport.actor(db, actor, "MANAGER");
        String code = "SP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT);
        String barcode = PosSupport.text(form.barcode(), "Mã vạch", 50);
        String name = PosSupport.text(form.name(), "Tên sản phẩm", 200);
        var unit = db.units().stream().filter(row -> form.unitId() == null
                ? "CAI".equals(row.get("code")) : Long.valueOf(PosSupport.id(row, "id")).equals(form.unitId()))
                .findFirst().orElseThrow(() -> new BusinessException("Đơn vị không hợp lệ hoặc chưa được cấu hình."));
        var category = db.categories().stream().filter(row -> form.categoryId() == null
                ? "UNCLASSIFIED".equals(row.get("code")) : Long.valueOf(PosSupport.id(row, "id")).equals(form.categoryId()))
                .findFirst().orElseThrow(() -> new BusinessException("Nhóm hàng không hợp lệ hoặc đã ngừng hoạt động."));
        BigDecimal price = PosSupport.money(form.price(), "Đơn giá");
        if (price.signum() <= 0) {
            throw new BusinessException("Đơn giá phải lớn hơn 0.");
        }
        BigDecimal stock = PosSupport.quantity(form.stock(), ((Number) unit.get("quantity_scale")).intValue(), "Tồn kho");
        // Barcode and SKU share the scan field: prevent ambiguous cross-column matches as well.
        if (db.products(null).stream().anyMatch(row -> code.equalsIgnoreCase(row.get("code").toString())
                || barcode.equalsIgnoreCase(row.get("barcode").toString())
                || code.equalsIgnoreCase(row.get("barcode").toString())
                || barcode.equalsIgnoreCase(row.get("code").toString()))) {
            throw new BusinessException("Mã vạch hoặc SKU đã tồn tại.");
        }
        String image = form.imageUrl() == null ? "" : form.imageUrl().strip();
        if (!image.isEmpty()) {
            try {
                URI uri = URI.create(image);
                if (image.length() > 500 || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Ảnh sản phẩm phải là URL HTTPS hợp lệ, tối đa 500 ký tự.");
            }
        }
        return db.createProduct(code, barcode, name, PosSupport.id(category, "id"), PosSupport.id(unit, "id"), price, stock,
                image.isEmpty() ? null : image, PosSupport.now());
    }
}
