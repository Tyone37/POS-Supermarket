package pos.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pos.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = "unit")
    Page<Product> findAll(Pageable pageable);

    @Query("select p.id from Product p where p.barcode = :code or p.code = :code")
    List<Long> findIdsByBarcodeOrCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findLockedById(@Param("id") Long id);
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.status = 'ACTIVE' AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:keyword = '' OR p.code = :code OR
                   LOWER(REPLACE(REPLACE(p.name, 'đ', 'd'), 'Đ', 'D')) COLLATE utf8mb4_unicode_ci
                       LIKE :pattern ESCAPE '!')
            ORDER BY p.id DESC
            """, countQuery = """
            SELECT COUNT(*) FROM products p
            WHERE p.status = 'ACTIVE' AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:keyword = '' OR p.code = :code OR
                   LOWER(REPLACE(REPLACE(p.name, 'đ', 'd'), 'Đ', 'D')) COLLATE utf8mb4_unicode_ci
                       LIKE :pattern ESCAPE '!')
            """, nativeQuery = true)
    Page<Product> searchActive(@Param("categoryId") Long categoryId, @Param("keyword") String keyword,
            @Param("code") String code, @Param("pattern") String pattern, Pageable pageable);
}
