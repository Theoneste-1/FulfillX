package com.fulfillx.shipment.domain;

import java.security.SecureRandom;

public final class TrackingNumbers {
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TrackingNumbers() {
    }

    public static String fulfillxLogistics() {
        StringBuilder suffix = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            suffix.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return "FX-FXL-" + suffix;
    }
}
