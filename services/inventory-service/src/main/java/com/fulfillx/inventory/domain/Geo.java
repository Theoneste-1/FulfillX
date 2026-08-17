package com.fulfillx.inventory.domain;

import java.util.Locale;
import java.util.Map;

public final class Geo {
    public record Coord(double latitude, double longitude) {
    }

    private static final Map<String, Coord> COUNTRY_CENTROIDS = Map.of(
            "RW", new Coord(-1.9403, 29.8739),
            "KE", new Coord(-0.0236, 37.9062),
            "UG", new Coord(1.3733, 32.2903),
            "TZ", new Coord(-6.3690, 34.8888),
            "US", new Coord(39.8283, -98.5795)
    );

    private Geo() {
    }

    public static Coord countryCentroid(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return COUNTRY_CENTROIDS.get(countryCode.trim().toUpperCase(Locale.ROOT));
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * earthRadiusKm * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static double normalizedDistance(double km) {
        return Math.min(1.0, km / 20_000.0);
    }
}
