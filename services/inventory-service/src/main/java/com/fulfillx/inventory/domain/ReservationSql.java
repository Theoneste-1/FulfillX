package com.fulfillx.inventory.domain;

/**
 * Race-safe reservation SQL (ADR-004). {@code rowCount == 0} means this warehouse
 * cannot take the SKU (insufficient available stock or concurrent reservation).
 */
public final class ReservationSql {
    public static final String CONDITIONAL_RESERVE = """
            UPDATE inventory
            SET quantity_reserved = quantity_reserved + :qty,
                version = version + 1,
                updated_at = now()
            WHERE warehouse_id = :wid
              AND product_id = :pid
              AND (quantity_on_hand - quantity_reserved) >= :qty
            """;

    public static final String RELEASE_RESERVED = """
            UPDATE inventory
            SET quantity_reserved = quantity_reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE id = :id
              AND quantity_reserved >= :qty
            """;

    public static final String CONSUME_SHIPPED = """
            UPDATE inventory
            SET quantity_on_hand = quantity_on_hand - :qty,
                quantity_reserved = quantity_reserved - :qty,
                version = version + 1,
                updated_at = now()
            WHERE id = :id
              AND quantity_reserved >= :qty
              AND quantity_on_hand >= :qty
            """;

    private ReservationSql() {
    }
}
