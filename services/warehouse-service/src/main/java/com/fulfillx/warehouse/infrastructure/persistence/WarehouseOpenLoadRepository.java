package com.fulfillx.warehouse.infrastructure.persistence;

import com.fulfillx.warehouse.domain.WarehouseOpenLoad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseOpenLoadRepository extends JpaRepository<WarehouseOpenLoad, UUID> {
}
