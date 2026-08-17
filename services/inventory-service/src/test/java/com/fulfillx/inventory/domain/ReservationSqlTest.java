package com.fulfillx.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationSqlTest {
    @Test
    void documentsConditionalUpdateFromAdr004() {
        String sql = ReservationSql.CONDITIONAL_RESERVE.replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("UPDATE inventory");
        assertThat(sql).contains("quantity_reserved = quantity_reserved + :qty");
        assertThat(sql).contains("version = version + 1");
        assertThat(sql).contains("updated_at = now()");
        assertThat(sql).contains("warehouse_id = :wid");
        assertThat(sql).contains("product_id = :pid");
        assertThat(sql).contains("(quantity_on_hand - quantity_reserved) >= :qty");
    }
}
