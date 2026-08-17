package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.ProductMetrics;
import com.fulfillx.analytics.domain.ProductMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, ProductMetricsId> {
    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, sku, date, units_sold, units_reserved)
            VALUES (:productId, :sku, :date, :sold, :reserved)
            ON CONFLICT (product_id, date) DO UPDATE SET
                sku = COALESCE(EXCLUDED.sku, product_metrics.sku),
                units_sold = product_metrics.units_sold + EXCLUDED.units_sold,
                units_reserved = product_metrics.units_reserved + EXCLUDED.units_reserved
            """, nativeQuery = true)
    void increment(
            @Param("productId") UUID productId,
            @Param("sku") String sku,
            @Param("date") LocalDate date,
            @Param("sold") int sold,
            @Param("reserved") int reserved
    );

    @Query(value = """
            SELECT product_id, MAX(sku) AS sku, SUM(units_sold) AS units
            FROM product_metrics
            GROUP BY product_id
            ORDER BY units DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topProducts(@Param("limit") int limit);
}
