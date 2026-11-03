package pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.CategoryStatus;
import pos.entity.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByIdAndStatus(Long id, CategoryStatus status);

    Optional<ProductCategory> findByCodeAndStatus(String code, CategoryStatus status);

    List<ProductCategory> findByStatus(CategoryStatus status, Sort sort);
}
