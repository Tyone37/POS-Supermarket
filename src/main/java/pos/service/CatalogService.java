package pos.service;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.Forms;
import pos.dto.ProductEditForm;
import pos.dto.ProductStatusForm;
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
        var values = validate(form, code, null);
        return db.createProduct(code, values.barcode(), values.name(), values.categoryId(), values.unitId(),
                values.price(), values.stock(), values.imageUrl(), PosSupport.now());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> editable(String username, long id) {
        var actor = db.account(username, false)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền sửa sản phẩm."));
        if (!"ACTIVE".equals(actor.get("status")) || !"MANAGER".equals(actor.get("role_code"))) {
            throw new AccessDeniedException("Không có quyền sửa sản phẩm.");
        }
        var product = new LinkedHashMap<>(product(id, false));
        product.put("unit_locked", db.hasProductHistory(id));
        return product;
    }

    @Transactional
    public void update(String username, long id, ProductEditForm form) {
        PosSupport.actor(db, username, "MANAGER");
        var current = product(id, true);
        checkRevision(form.getExpectedUpdatedAt(), (Timestamp) current.get("updated_at"));
        if (form.getCategoryId() == null || form.getUnitId() == null) {
            throw new BusinessException("Vui lòng chọn nhóm hàng và đơn vị.");
        }
        var values = validate(form.product(), (String) current.get("code"), id);
        if (values.unitId() != PosSupport.id(current, "unit_id") && db.hasProductHistory(id)) {
            throw new BusinessException("Không thể đổi đơn vị của sản phẩm đã có lịch sử giao dịch hoặc tồn kho.");
        }
        if (values.stock().compareTo((BigDecimal) current.get("reserved_quantity")) < 0) {
            throw new BusinessException("Tồn kho không được thấp hơn số lượng đang giữ cho giao dịch.");
        }
        db.updateProduct(id, values.barcode(), values.name(), values.categoryId(), values.unitId(),
                values.price(), values.stock(), values.imageUrl(), PosSupport.now());
    }

    @Transactional
    public void changeStatus(String username, long id, ProductStatusForm form) {
        PosSupport.actor(db, username, "MANAGER");
        if (form.getStatus() == null) {
            throw new BusinessException("Trạng thái bán không hợp lệ.");
        }
        var current = product(id, true);
        String target = form.getStatus().name();
        // Repeating the same desired state keeps the original stopped_at and updated_at.
        if (target.equals(current.get("status"))) {
            return;
        }
        checkRevision(form.getExpectedUpdatedAt(), (Timestamp) current.get("updated_at"));
        db.changeProductStatus(id, target, PosSupport.now());
    }

    private Map<String, Object> product(long id, boolean lock) {
        if (id <= 0) {
            throw new BusinessException("Mã sản phẩm không hợp lệ.");
        }
        return db.product(id, lock).orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));
    }

    private void checkRevision(String expected, Timestamp current) {
        try {
            if (expected == null || !Instant.parse(expected).equals(current.toInstant())) {
                throw new BusinessException("Sản phẩm hoặc tồn kho đã thay đổi. Vui lòng tải lại trước khi lưu.");
            }
        } catch (DateTimeParseException exception) {
            throw new BusinessException("Vui lòng tải lại sản phẩm trước khi lưu.");
        }
    }

    private ProductValues validate(Forms.Product form, String code, Long excludedId) {
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
        if (db.products(null).stream().filter(row -> excludedId == null || PosSupport.id(row, "id") != excludedId)
                .anyMatch(row -> code.equalsIgnoreCase(row.get("code").toString())
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
        return new ProductValues(barcode, name, PosSupport.id(category, "id"), PosSupport.id(unit, "id"),
                price, stock, image.isEmpty() ? null : image);
    }

    private record ProductValues(String barcode, String name, long categoryId, long unitId,
                                 BigDecimal price, BigDecimal stock, String imageUrl) {
    }
}
