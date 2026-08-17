package com.fulfillx.shipment.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ShipmentTransitions {
    private static final Map<Shipment.Status, Set<Shipment.Status>> ALLOWED = new EnumMap<>(Shipment.Status.class);

    static {
        ALLOWED.put(Shipment.Status.CREATED, EnumSet.of(Shipment.Status.ASSIGNED, Shipment.Status.FAILED, Shipment.Status.DELAYED));
        ALLOWED.put(Shipment.Status.ASSIGNED, EnumSet.of(Shipment.Status.PICKED_UP, Shipment.Status.FAILED, Shipment.Status.DELAYED));
        ALLOWED.put(Shipment.Status.PICKED_UP, EnumSet.of(Shipment.Status.IN_TRANSIT, Shipment.Status.FAILED, Shipment.Status.DELAYED));
        ALLOWED.put(Shipment.Status.IN_TRANSIT, EnumSet.of(Shipment.Status.OUT_FOR_DELIVERY, Shipment.Status.DELAYED, Shipment.Status.FAILED));
        ALLOWED.put(Shipment.Status.OUT_FOR_DELIVERY, EnumSet.of(Shipment.Status.DELIVERED, Shipment.Status.DELAYED, Shipment.Status.FAILED));
        ALLOWED.put(Shipment.Status.DELAYED, EnumSet.of(
                Shipment.Status.ASSIGNED,
                Shipment.Status.PICKED_UP,
                Shipment.Status.IN_TRANSIT,
                Shipment.Status.OUT_FOR_DELIVERY,
                Shipment.Status.FAILED
        ));
        ALLOWED.put(Shipment.Status.DELIVERED, EnumSet.noneOf(Shipment.Status.class));
        ALLOWED.put(Shipment.Status.FAILED, EnumSet.noneOf(Shipment.Status.class));
    }

    private static final Shipment.Status[] AUTO_PROGRESS = {
            Shipment.Status.CREATED,
            Shipment.Status.ASSIGNED,
            Shipment.Status.PICKED_UP,
            Shipment.Status.IN_TRANSIT,
            Shipment.Status.OUT_FOR_DELIVERY,
            Shipment.Status.DELIVERED
    };

    private ShipmentTransitions() {
    }

    public static boolean canTransition(Shipment.Status from, Shipment.Status to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static Shipment.Status nextAutoProgress(Shipment.Status current) {
        for (int i = 0; i < AUTO_PROGRESS.length - 1; i++) {
            if (AUTO_PROGRESS[i] == current) {
                return AUTO_PROGRESS[i + 1];
            }
        }
        return null;
    }
}
