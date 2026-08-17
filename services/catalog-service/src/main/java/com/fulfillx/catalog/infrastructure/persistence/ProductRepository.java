package com.fulfillx.catalog.infrastructure.persistence;

import com.fulfillx.catalog.domain.Product;
import com.fulfillx.catalog.domain.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    Optional<Product> findBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE (:category IS NULL OR p.category = :category)
              AND (:active IS NULL OR p.active = :active)
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Product> search(
            @Param("category") ProductCategory category,
            @Param("active") Boolean active,
            @Param("q") String q,
            Pageable pageable
    );
}
