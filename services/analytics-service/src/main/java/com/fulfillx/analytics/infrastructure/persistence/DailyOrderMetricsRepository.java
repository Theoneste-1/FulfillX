package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.DailyOrderMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DailyOrderMetricsRepository extends JpaRepository<DailyOrderMetrics, LocalDate> {
    List<DailyOrderMetrics> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    @Modifying
    @Query(value = """
            INSERT INTO daily_order_metrics
                (date, orders_created, orders_completed, orders_cancelled, orders_failed, revenue, fulfillment_time_seconds)
            VALUES
                (:date, :created, :completed, :cancelled, :failed, :revenue, :fulfillment)
            ON CONFLICT (date) DO UPDATE SET
                orders_created = daily_order_metrics.orders_created + EXCLUDED.orders_created,
                orders_completed = daily_order_metrics.orders_completed + EXCLUDED.orders_completed,
                orders_cancelled = daily_order_metrics.orders_cancelled + EXCLUDED.orders_cancelled,
                orders_failed = daily_order_metrics.orders_failed + EXCLUDED.orders_failed,
                revenue = daily_order_metrics.revenue + EXCLUDED.revenue,
                fulfillment_time_seconds = COALESCE(daily_order_metrics.fulfillment_time_seconds, 0)
                    + COALESCE(EXCLUDED.fulfillment_time_seconds, 0)
            """, nativeQuery = true)
    void increment(
            @Param("date") LocalDate date,
            @Param("created") int created,
            @Param("completed") int completed,
            @Param("cancelled") int cancelled,
            @Param("failed") int failed,
            @Param("revenue") BigDecimal revenue,
            @Param("fulfillment") long fulfillment
    );
}
