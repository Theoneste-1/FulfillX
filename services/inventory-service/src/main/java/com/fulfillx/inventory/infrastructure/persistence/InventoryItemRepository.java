package com.fulfillx.inventory.infrastructure.persistence;

import com.fulfillx.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    Optional<InventoryItem> findByWarehouseIdAndSkuIgnoreCase(UUID warehouseId, String sku);

    boolean existsByWarehouseIdAndSkuIgnoreCase(UUID warehouseId, String sku);

    List<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findByWarehouseId(UUID warehouseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET quantity_reserved = quantity_reserved + :qty,
                version = version + 1,
                updated_at = now()
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND (quantity_on_hand - quantity_reserved) >= :qty
            """, nativeQuery = true)
    int tryReserve(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET quantity_reserved = quantity_reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND quantity_reserved >= :qty
            """, nativeQuery = true)
    int release(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
            SET quantity_on_hand = quantity_on_hand - :qty,
                quantity_reserved = quantity_reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND quantity_reserved >= :qty
              AND quantity_on_hand >= :qty
            """, nativeQuery = true)
    int consumeShipped(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId, @Param("qty") int qty);
}
