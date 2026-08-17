package com.fulfillx.order.application;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class OrderNumberGenerator {
    private static final char[] ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderNumberGenerator() {
    }

    public static String next() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return "ORD-" + DAY.format(Instant.now()) + "-" + suffix;
    }
}
