package pos.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

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
        String term = PosSupport.normalized(query);
        return db.products(category).stream()
                .filter(row -> !selling || "ACTIVE".equals(row.get("status")))
                .filter(row -> PosSupport.normalized(row.get("name").toString()).contains(term)
                        || PosSupport.normalized(row.get("code").toString()).contains(term)
                        || row.get("barcode").toString().contains(term))
                .limit(100).toList();
    }

    public List<Map<String, Object>> categories() { return db.categories(); }
    public List<Map<String, Object>> units() { return db.units(); }

    @Transactional
    public long create(String actor, Forms.Product form) {
        PosSupport.actor(db, actor, "MANAGER");
        String code = PosSupport.text(form.code(), "Mã SKU", 20);
        String barcode = PosSupport.text(form.barcode(), "Mã vạch", 50);
        String name = PosSupport.text(form.name(), "Tên sản phẩm", 200);
        var unit = db.units().stream().filter(row -> Long.valueOf(PosSupport.id(row, "id")).equals(form.unitId()))
                .findFirst().orElseThrow(() -> new BusinessException("Đơn vị không hợp lệ."));
        if (db.categories().stream().noneMatch(row -> Long.valueOf(PosSupport.id(row, "id")).equals(form.categoryId()))) {
            throw new BusinessException("Nhóm hàng không hợp lệ hoặc đã ngừng hoạt động.");
        }
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
        return db.createProduct(code, barcode, name, form.categoryId(), form.unitId(), price, stock,
                image.isEmpty() ? null : image, PosSupport.now());
    }
}
