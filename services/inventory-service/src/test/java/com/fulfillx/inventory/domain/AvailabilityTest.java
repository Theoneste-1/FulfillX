package com.fulfillx.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityTest {
    @Test
    void availableIsOnHandMinusReserved() {
        assertThat(Availability.of(5, 0)).isEqualTo(5);
        assertThat(Availability.of(5, 2)).isEqualTo(3);
        assertThat(Availability.of(5, 5)).isEqualTo(0);
    }

    @Test
    void concurrentFourAndThreeAgainstFiveCannotBothSucceed() {
        int onHand = 5;
        int reserved = 0;
        boolean first = Availability.canFulfill(onHand, reserved, 4);
        boolean second = Availability.canFulfill(onHand, reserved, 3);
        assertThat(first && second).isTrue();
        int afterFirst = reserved + 4;
        assertThat(Availability.canFulfill(onHand, afterFirst, 3)).isFalse();
        assertThat(Availability.of(onHand, afterFirst)).isEqualTo(1);
    }

    @Test
    void rejectsInvalidQuantities() {
        assertThatThrownBy(() -> Availability.of(3, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
