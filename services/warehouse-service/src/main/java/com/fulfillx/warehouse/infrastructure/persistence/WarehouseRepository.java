package com.fulfillx.warehouse.infrastructure.persistence;

import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    boolean existsByCode(String code);

    Optional<Warehouse> findByCode(String code);

    List<Warehouse> findByStatus(WarehouseStatus status);

    List<Warehouse> findByCountry(String country);
}
