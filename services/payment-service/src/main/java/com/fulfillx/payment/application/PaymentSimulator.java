package com.fulfillx.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PaymentSimulator {
    public static final String SIMULATED_DECLINE = "SIMULATED_DECLINE";
    public static final String DETERMINISTIC_DECLINE = "DETERMINISTIC_DECLINE";

    private PaymentSimulator() {
    }

    public static boolean shouldFail(boolean paymentFail, BigDecimal amount) {
        return paymentFail || centsMod100(amount) == 13;
    }

    public static String failureReason(boolean paymentFail, BigDecimal amount) {
        if (paymentFail) {
            return SIMULATED_DECLINE;
        }
        if (centsMod100(amount) == 13) {
            return DETERMINISTIC_DECLINE;
        }
        return null;
    }

    static long centsMod100(BigDecimal amount) {
        long cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        long mod = cents % 100;
        return mod < 0 ? mod + 100 : mod;
    }
}
