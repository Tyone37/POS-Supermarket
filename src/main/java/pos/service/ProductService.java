package pos.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Validator;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.CreateProductRequest;
import pos.dto.ProductOption;
import pos.dto.ProductRow;
import pos.entity.CategoryStatus;
import pos.entity.InventoryMovement;
import pos.entity.Product;
import pos.entity.ProductCategory;
import pos.entity.Unit;
import pos.exception.ProductValidationException;
import pos.repository.InventoryMovementRepository;
import pos.repository.ProductCategoryRepository;
import pos.repository.ProductRepository;
import pos.repository.UnitRepository;

@Service
@PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
public class ProductService {

    private final ProductRepository products;
    private final ProductCategoryRepository categories;
    private final UnitRepository units;
    private final InventoryMovementRepository movements;
    private final Validator validator;

    public ProductService(ProductRepository products, ProductCategoryRepository categories, UnitRepository units,
            InventoryMovementRepository movements, Validator validator) {
        this.products = products;
        this.categories = categories;
        this.units = units;
        this.movements = movements;
        this.validator = validator;
    }

    @Transactional
    public void create(CreateProductRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validator.validate(request).forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        if (!errors.isEmpty()) {
            throw new ProductValidationException(errors);
        }

        ProductCategory category = (request.categoryId() == null
                ? categories.findByCodeAndStatus("UNCLASSIFIED", CategoryStatus.ACTIVE)
                : categories.findByIdAndStatus(request.categoryId(), CategoryStatus.ACTIVE))
                .orElseThrow(() -> new ProductValidationException(
                        Map.of("categoryId", "Loại hàng không hợp lệ hoặc chưa được cấu hình")));
        Unit unit = (request.unitId() == null ? units.findByCode("CAI") : units.findById(request.unitId()))
                .orElseThrow(() -> new ProductValidationException(
                        Map.of("unitId", "Đơn vị không hợp lệ hoặc chưa được cấu hình")));
        if (request.stockQuantity().stripTrailingZeros().scale() > unit.getQuantityScale()) {
            throw new ProductValidationException(Map.of("stockQuantity",
                    "Số lượng không đúng độ lẻ của đơn vị đã chọn"));
        }

        String code = "SP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18)
                .toUpperCase(Locale.ROOT);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Product product = new Product(code, request.barcode(), request.name(), category, unit,
                request.salePrice(), request.stockQuantity(), now);
        try {
            products.saveAndFlush(product);
            if (request.stockQuantity().signum() > 0) {
                movements.saveAndFlush(new InventoryMovement(product, now));
            }
        } catch (DataIntegrityViolationException exception) {
            if (isConstraint(exception, "barcode")) {
                throw new ProductValidationException(Map.of("barcode", "Mã vạch đã tồn tại"));
            }
            if (isConstraint(exception, "code")) {
                throw new ProductValidationException(Map.of("", "Chưa tạo được mã sản phẩm. Vui lòng thử lại."));
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductRow> list(int page) {
        return products.findAll(PageRequest.of(Math.max(page, 0), 20,
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(product -> new ProductRow(product.getCode(), product.getBarcode(), product.getName(),
                        product.getUnit().getName(), product.getSalePrice(), product.getStockQuantity(),
                        product.getStatus()));
    }

    @Transactional(readOnly = true)
    public List<ProductOption> categories() {
        return categories.findByStatus(CategoryStatus.ACTIVE, Sort.by("name", "id")).stream()
                .map(category -> new ProductOption(category.getId(), category.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductOption> units() {
        return units.findAll(Sort.by("name", "id")).stream()
                .map(unit -> new ProductOption(unit.getId(), unit.getName())).toList();
    }

    private boolean isConstraint(DataIntegrityViolationException exception, String column) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                return constraint != null && (constraint.equals(column) || constraint.endsWith("." + column));
            }
        }
        return false;
    }
}
