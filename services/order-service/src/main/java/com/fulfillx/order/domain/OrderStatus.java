package com.fulfillx.order.domain;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAID,
    INVENTORY_PENDING,
    INVENTORY_FAILED,
    RESERVED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPENSATION_REQUIRED,
    REFUND_PENDING,
    CANCELLED,
    FAILED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.ofEntries(
            Map.entry(CREATED, Set.of(PAYMENT_PENDING, CANCELLED)),
            Map.entry(PAYMENT_PENDING, Set.of(PAID, PAYMENT_FAILED, CANCELLED)),
            Map.entry(PAYMENT_FAILED, Set.of()),
            Map.entry(PAID, Set.of(INVENTORY_PENDING, COMPENSATION_REQUIRED, CANCELLED)),
            Map.entry(INVENTORY_PENDING, Set.of(RESERVED, INVENTORY_FAILED)),
            Map.entry(INVENTORY_FAILED, Set.of(COMPENSATION_REQUIRED)),
            Map.entry(RESERVED, Set.of(PROCESSING, CANCELLED)),
            Map.entry(PROCESSING, Set.of(SHIPPED, CANCELLED)),
            Map.entry(SHIPPED, Set.of(DELIVERED)),
            Map.entry(DELIVERED, Set.of()),
            Map.entry(COMPENSATION_REQUIRED, Set.of(REFUND_PENDING)),
            Map.entry(REFUND_PENDING, Set.of(CANCELLED)),
            Map.entry(CANCELLED, Set.of()),
            Map.entry(FAILED, Set.of())
    );

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public void assertCanTransition(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new FulfillxException(
                    ErrorCode.ILLEGAL_ORDER_TRANSITION,
                    "Cannot transition order from " + this + " to " + target
            );
        }
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED || this == PAYMENT_FAILED;
    }

    public boolean cancellable() {
        return this == CREATED || this == PAYMENT_PENDING || this == PAID || this == RESERVED || this == PROCESSING;
    }

    public static Set<OrderStatus> allowedTargets(OrderStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }
}
