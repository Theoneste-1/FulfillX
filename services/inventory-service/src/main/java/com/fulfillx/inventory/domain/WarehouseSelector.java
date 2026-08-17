package com.fulfillx.inventory.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WarehouseSelector {
    private WarehouseSelector() {
    }

    public record RequestItem(java.util.UUID productId, String sku, int quantity) {
    }

    public record RankedWarehouse(WarehouseCache warehouse, int availableSum, double score) {
    }

    public static double score(double distanceKm, double utilization, int availableSum) {
        double availabilityTerm = availableSum <= 0 ? 1.0 : (1.0 / availableSum);
        return Geo.normalizedDistance(distanceKm) * 0.6 + utilization * 0.3 + availabilityTerm * 0.1;
    }

    public static List<RankedWarehouse> rank(
            List<RankedWarehouse> eligible,
            String destinationCountry
    ) {
        Geo.Coord dest = Geo.countryCentroid(destinationCountry);
        return eligible.stream()
                .map(candidate -> new RankedWarehouse(
                        candidate.warehouse(),
                        candidate.availableSum(),
                        score(
                                distanceKm(candidate.warehouse(), dest),
                                candidate.warehouse().utilization(),
                                candidate.availableSum()
                        )
                ))
                .sorted(Comparator
                        .comparingDouble(RankedWarehouse::score)
                        .thenComparing(ranked -> ranked.warehouse().getCode(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static boolean passesHardFilters(WarehouseCache warehouse, String destinationCountry) {
        return warehouse.isOperational()
                && warehouse.supportsCountry(destinationCountry)
                && warehouse.hasCapacity();
    }

    private static double distanceKm(WarehouseCache warehouse, Geo.Coord dest) {
        if (dest == null || warehouse.getLatitude() == null || warehouse.getLongitude() == null) {
            return 0.0;
        }
        return Geo.haversineKm(
                warehouse.getLatitude(),
                warehouse.getLongitude(),
                dest.latitude(),
                dest.longitude()
        );
    }

    public static String normalizeCountry(String country) {
        return country == null ? null : country.trim().toUpperCase(Locale.ROOT);
    }
}
