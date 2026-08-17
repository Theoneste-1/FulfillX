package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.FactOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FactOrderRepository extends JpaRepository<FactOrder, UUID> {
    long countByFailedTrue();

    long countByDelayedTrue();

    @Query("SELECT COALESCE(SUM(f.totalAmount), 0) FROM FactOrder f WHERE f.paidAt IS NOT NULL")
    BigDecimal sumPaidRevenue();

    @Query("SELECT f.status, COUNT(f) FROM FactOrder f GROUP BY f.status")
    List<Object[]> countByStatus();

    @Query("""
            SELECT COUNT(f) FROM FactOrder f
            WHERE f.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED', 'PAYMENT_FAILED')
            """)
    long countPending();

    @Query("""
            SELECT COUNT(f) FROM FactOrder f
            WHERE f.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED', 'PAYMENT_FAILED')
              AND f.deliveredAt IS NULL
              AND f.createdAt < :cutoff
            """)
    long countStuck(Instant cutoff);

    @Query("""
            SELECT f.warehouseId, COUNT(f) FROM FactOrder f
            WHERE f.warehouseId IS NOT NULL
            GROUP BY f.warehouseId
            """)
    List<Object[]> countByWarehouse();
}
