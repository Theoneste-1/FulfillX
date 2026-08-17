package com.fulfillx.inventory.infrastructure.persistence;

import com.fulfillx.inventory.domain.WarehouseCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseCacheRepository extends JpaRepository<WarehouseCache, UUID> {
}
