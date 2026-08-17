package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.WarehouseMetrics;
import com.fulfillx.analytics.domain.WarehouseMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WarehouseMetricsRepository extends JpaRepository<WarehouseMetrics, WarehouseMetricsId> {
    @Modifying
    @Query(value = """
            INSERT INTO warehouse_metrics
                (warehouse_id, date, orders_processed, failure_rate_numerator, failure_rate_denominator)
            VALUES
                (:warehouseId, :date, :processed, :failNum, :failDen)
            ON CONFLICT (warehouse_id, date) DO UPDATE SET
                orders_processed = warehouse_metrics.orders_processed + EXCLUDED.orders_processed,
                failure_rate_numerator = warehouse_metrics.failure_rate_numerator + EXCLUDED.failure_rate_numerator,
                failure_rate_denominator = warehouse_metrics.failure_rate_denominator + EXCLUDED.failure_rate_denominator
            """, nativeQuery = true)
    void increment(
            @Param("warehouseId") UUID warehouseId,
            @Param("date") LocalDate date,
            @Param("processed") int processed,
            @Param("failNum") int failNum,
            @Param("failDen") int failDen
    );

    @Query("""
            SELECT w.warehouseId, SUM(w.ordersProcessed), SUM(w.failureRateNumerator), SUM(w.failureRateDenominator)
            FROM WarehouseMetrics w
            GROUP BY w.warehouseId
            """)
    List<Object[]> totalsByWarehouse();
}
