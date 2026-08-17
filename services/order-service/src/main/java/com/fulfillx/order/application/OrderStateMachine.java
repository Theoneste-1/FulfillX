package com.fulfillx.order.application;

import com.fulfillx.order.domain.OrderStatus;

import java.util.Set;

public final class OrderStateMachine {
    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return from.canTransitionTo(to);
    }

    public static boolean canCancel(OrderStatus status) {
        return status.cancellable();
    }

    public static void require(OrderStatus from, OrderStatus to) {
        from.assertCanTransition(to);
    }

    public static Set<OrderStatus> allowedTargets(OrderStatus from) {
        return OrderStatus.allowedTargets(from);
    }
}
