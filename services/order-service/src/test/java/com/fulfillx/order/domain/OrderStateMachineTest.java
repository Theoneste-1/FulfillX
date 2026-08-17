package com.fulfillx.order.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {
    @Test
    void happyPathTransitions() {
        OrderStatus status = OrderStatus.CREATED;
        status.assertCanTransition(OrderStatus.PAYMENT_PENDING);
        OrderStatus.PAYMENT_PENDING.assertCanTransition(OrderStatus.PAID);
        OrderStatus.PAID.assertCanTransition(OrderStatus.INVENTORY_PENDING);
        OrderStatus.INVENTORY_PENDING.assertCanTransition(OrderStatus.RESERVED);
        OrderStatus.RESERVED.assertCanTransition(OrderStatus.PROCESSING);
        OrderStatus.PROCESSING.assertCanTransition(OrderStatus.SHIPPED);
        OrderStatus.SHIPPED.assertCanTransition(OrderStatus.DELIVERED);
    }

    @Test
    void compensationPath() {
        OrderStatus.INVENTORY_FAILED.assertCanTransition(OrderStatus.COMPENSATION_REQUIRED);
        OrderStatus.COMPENSATION_REQUIRED.assertCanTransition(OrderStatus.REFUND_PENDING);
        OrderStatus.REFUND_PENDING.assertCanTransition(OrderStatus.CANCELLED);
    }

    @Test
    void cannotSkipToDelivered() {
        assertThatThrownBy(() -> OrderStatus.CREATED.assertCanTransition(OrderStatus.DELIVERED))
                .hasMessageContaining("Cannot transition");
        assertThat(OrderStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(OrderStatus.SHIPPED.cancellable()).isFalse();
    }
}
