package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.WarehouseSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseSnapshotRepository extends JpaRepository<WarehouseSnapshot, UUID> {
}
