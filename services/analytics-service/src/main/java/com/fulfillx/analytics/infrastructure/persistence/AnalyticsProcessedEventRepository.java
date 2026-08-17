package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.common.idempotency.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AnalyticsProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    @Query("""
            SELECT COUNT(p) FROM ProcessedEvent p
            WHERE p.eventType = :eventType AND p.processedAt >= :since
            """)
    long countByTypeSince(@Param("eventType") String eventType, @Param("since") Instant since);
}
