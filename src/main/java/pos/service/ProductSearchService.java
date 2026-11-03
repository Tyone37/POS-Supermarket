package pos.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.ProductOption;
import pos.dto.ProductSearchRow;
import pos.entity.CategoryStatus;
import pos.exception.FormValidationException;
import pos.repository.ProductCategoryRepository;
import pos.repository.ProductRepository;

@Service
@PreAuthorize("hasAuthority('SALE_USE')")
public class ProductSearchService {
    private final SaleService sales;
    private final CurrentActorService actors;
    private final ProductRepository products;
    private final ProductCategoryRepository categories;

    public ProductSearchService(SaleService sales, CurrentActorService actors, ProductRepository products,
            ProductCategoryRepository categories) {
        this.sales = sales;
        this.actors = actors;
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public Page<ProductSearchRow> search(Long orderId, String query, Long categoryId, int page) {
        sales.view(orderId);
        if (categoryId != null && categoryId <= 0) {
            throw new FormValidationException(Map.of("categoryId", "Danh mục không hợp lệ."));
        }
        String code = query == null ? "" : query.strip();
        if (code.length() > 200) {
            throw new FormValidationException(Map.of("query", "Từ khóa không được vượt quá 200 ký tự."));
        }
        if (page < 0) {
            throw new FormValidationException(Map.of("page", "Trang không hợp lệ."));
        }
        String keyword = Normalizer.normalize(code, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return products.searchActive(categoryId, keyword, code, pattern, PageRequest.of(page, 20))
                .map(product -> new ProductSearchRow(product.getId(), product.getCode(), product.getName(),
                        product.getUnit().getName(), product.getSalePrice(), product.getStockQuantity()));
    }

    @Transactional(readOnly = true)
    public List<ProductOption> categories() {
        actors.requireEmployee();
        return categories.findByStatus(CategoryStatus.ACTIVE, Sort.by("name").and(Sort.by("id"))).stream()
                .map(category -> new ProductOption(category.getId(), category.getName())).toList();
    }
}
