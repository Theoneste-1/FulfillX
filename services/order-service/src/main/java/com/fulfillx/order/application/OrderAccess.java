package com.fulfillx.order.application;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.security.Roles;
import com.fulfillx.order.domain.Order;
import com.fulfillx.security.FulfillxPrincipal;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public final class OrderAccess {
    private OrderAccess() {
    }

    public static boolean hasRole(FulfillxPrincipal principal, String role) {
        return principal.roles() != null && principal.roles().contains(role);
    }

    public static boolean hasAnyRole(FulfillxPrincipal principal, String... roles) {
        for (String role : roles) {
            if (hasRole(principal, role)) {
                return true;
            }
        }
        return false;
    }

    public static void requireRead(Order order, FulfillxPrincipal principal) {
        boolean privileged = hasAnyRole(principal, Roles.SUPPORT, Roles.ADMIN, Roles.LOGISTICS_OPERATOR);
        if (!privileged && !order.getCustomerId().equals(principal.userId())) {
            throw new AccessDeniedException("Not allowed to access this order");
        }
    }

    public static void requireCancel(Order order, FulfillxPrincipal principal) {
        boolean privileged = hasAnyRole(principal, Roles.SUPPORT, Roles.ADMIN);
        if (!privileged && !order.getCustomerId().equals(principal.userId())) {
            throw new AccessDeniedException("Not allowed to cancel this order");
        }
    }

    public static UUID customerIdForCreate(FulfillxPrincipal principal, UUID bodyCustomerId, UUID queryCustomerId) {
        if (hasRole(principal, Roles.ADMIN)) {
            if (queryCustomerId != null) {
                return queryCustomerId;
            }
            if (bodyCustomerId != null) {
                return bodyCustomerId;
            }
            return principal.userId();
        }
        return principal.userId();
    }

    public static void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new FulfillxException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "Idempotency-Key header is required");
        }
    }
}
