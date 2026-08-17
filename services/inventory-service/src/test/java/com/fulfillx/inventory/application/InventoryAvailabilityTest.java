package com.fulfillx.inventory.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAvailabilityTest {
    @Test
    void availableIsOnHandMinusReserved() {
        int onHand = 5;
        int reserved = 2;
        assertThat(onHand - reserved).isEqualTo(3);
        assertThat(onHand - reserved >= 4).isFalse();
        assertThat(onHand - reserved >= 3).isTrue();
    }
}
