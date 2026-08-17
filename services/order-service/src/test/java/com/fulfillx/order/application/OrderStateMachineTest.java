package com.fulfillx.order.application;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "CREATED,PAYMENT_PENDING",
            "CREATED,CANCELLED",
            "PAYMENT_PENDING,PAID",
            "PAYMENT_PENDING,PAYMENT_FAILED",
            "PAYMENT_PENDING,CANCELLED",
            "PAID,INVENTORY_PENDING",
            "PAID,COMPENSATION_REQUIRED",
            "PAID,CANCELLED",
            "INVENTORY_PENDING,RESERVED",
            "INVENTORY_PENDING,INVENTORY_FAILED",
            "INVENTORY_FAILED,COMPENSATION_REQUIRED",
            "RESERVED,PROCESSING",
            "RESERVED,CANCELLED",
            "PROCESSING,SHIPPED",
            "PROCESSING,CANCELLED",
            "SHIPPED,DELIVERED",
            "COMPENSATION_REQUIRED,REFUND_PENDING",
            "REFUND_PENDING,CANCELLED"
    })
    void allowsDocumentedTransitions(OrderStatus from, OrderStatus to) {
        assertThat(OrderStateMachine.canTransition(from, to)).isTrue();
        OrderStateMachine.require(from, to);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED,PAID",
            "CREATED,SHIPPED",
            "PAYMENT_PENDING,INVENTORY_PENDING",
            "PAYMENT_FAILED,PAID",
            "PAYMENT_FAILED,CANCELLED",
            "PAID,RESERVED",
            "INVENTORY_PENDING,CANCELLED",
            "INVENTORY_PENDING,PAID",
            "INVENTORY_FAILED,CANCELLED",
            "RESERVED,INVENTORY_FAILED",
            "RESERVED,SHIPPED",
            "PROCESSING,DELIVERED",
            "SHIPPED,CANCELLED",
            "DELIVERED,CANCELLED",
            "COMPENSATION_REQUIRED,CANCELLED",
            "REFUND_PENDING,COMPENSATION_REQUIRED",
            "CANCELLED,CREATED",
            "FAILED,CANCELLED"
    })
    void rejectsIllegalTransitions(OrderStatus from, OrderStatus to) {
        assertThat(OrderStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> OrderStateMachine.require(from, to))
                .isInstanceOf(FulfillxException.class)
                .satisfies(ex -> assertThat(((FulfillxException) ex).code())
                        .isEqualTo(ErrorCode.ILLEGAL_ORDER_TRANSITION));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PAYMENT_FAILED", "DELIVERED", "CANCELLED", "FAILED"})
    void terminalStatusesHaveNoOutgoingTransitions(OrderStatus status) {
        assertThat(OrderStateMachine.allowedTargets(status)).isEmpty();
    }

    @Test
    void cancelIsAllowedOnlyFromDocumentedSources() {
        assertThat(OrderStateMachine.canCancel(OrderStatus.CREATED)).isTrue();
        assertThat(OrderStateMachine.canCancel(OrderStatus.PAYMENT_PENDING)).isTrue();
        assertThat(OrderStateMachine.canCancel(OrderStatus.PAID)).isTrue();
        assertThat(OrderStateMachine.canCancel(OrderStatus.RESERVED)).isTrue();
        assertThat(OrderStateMachine.canCancel(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStateMachine.canCancel(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStateMachine.canCancel(OrderStatus.INVENTORY_PENDING)).isFalse();
        assertThat(OrderStateMachine.canCancel(OrderStatus.DELIVERED)).isFalse();
    }
}
