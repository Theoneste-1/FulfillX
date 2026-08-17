package com.fulfillx.analytics.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PayloadMaps {
    private PayloadMaps() {
    }

    static String text(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    static UUID uuid(Map<String, Object> payload, String key) {
        String text = text(payload, key);
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    static BigDecimal decimal(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    static Integer integer(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static Instant instant(Map<String, Object> payload, String key, Instant fallback) {
        String text = text(payload, key);
        if (text == null) {
            return fallback;
        }
        try {
            return Instant.parse(text);
        } catch (Exception ex) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> listOfMaps(Map<String, Object> payload, String key) {
        if (payload == null) {
            return List.of();
        }
        Object value = payload.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    static Map<String, Object> orEmpty(Map<String, Object> payload) {
        return payload == null ? Collections.emptyMap() : payload;
    }
}
