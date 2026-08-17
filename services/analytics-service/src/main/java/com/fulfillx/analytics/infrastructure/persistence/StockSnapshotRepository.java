package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.StockSnapshot;
import com.fulfillx.analytics.domain.StockSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, StockSnapshotId> {
    List<StockSnapshot> findByAvailableLessThanEqual(int available);
}
