package com.fulfillx.inventory.infrastructure.persistence;

import com.fulfillx.inventory.domain.ReservationSql;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class InventoryReservationConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillx_inventory")
            .withUsername("fulfillx")
            .withPassword("fulfillx");

    @Test
    void exactlyOneOfTwoContendingBasketsSucceeds() throws Exception {
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO inventory (id, warehouse_id, product_id, sku, quantity_on_hand, quantity_reserved, version, updated_at)
                    VALUES (?, ?, ?, 'FX-MOUSE-01', 5, 0, 0, now())
                    """)) {
                insert.setObject(1, inventoryId);
                insert.setObject(2, warehouseId);
                insert.setObject(3, productId);
                insert.executeUpdate();
            }
        }

        String sql = """
                UPDATE inventory
                SET quantity_reserved = quantity_reserved + ?,
                    version = version + 1,
                    updated_at = now()
                WHERE warehouse_id = ?
                  AND product_id = ?
                  AND (quantity_on_hand - quantity_reserved) >= ?
                """;
        assertThat(ReservationSql.CONDITIONAL_RESERVE).contains("(quantity_on_hand - quantity_reserved) >= :qty");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> four = pool.submit(() -> reserve(sql, warehouseId, productId, 4, start));
            Future<Integer> three = pool.submit(() -> reserve(sql, warehouseId, productId, 3, start));
            start.countDown();
            int first = four.get(10, TimeUnit.SECONDS);
            int second = three.get(10, TimeUnit.SECONDS);
            successes.addAndGet(first);
            successes.addAndGet(second);
            assertThat(successes.get()).isEqualTo(1);
            assertThat(first + second).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); PreparedStatement select = connection.prepareStatement(
                "SELECT quantity_reserved FROM inventory WHERE id = ?"
        )) {
            select.setObject(1, inventoryId);
            try (ResultSet rs = select.executeQuery()) {
                assertThat(rs.next()).isTrue();
                int reserved = rs.getInt(1);
                assertThat(reserved).isIn(3, 4);
            }
        }
    }

    private static int reserve(String sql, UUID warehouseId, UUID productId, int qty, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, qty);
                statement.setObject(2, warehouseId);
                statement.setObject(3, productId);
                statement.setInt(4, qty);
                int updated = statement.executeUpdate();
                connection.commit();
                return updated;
            }
        }
    }
}
