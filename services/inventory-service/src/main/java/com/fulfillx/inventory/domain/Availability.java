package com.fulfillx.inventory.domain;

public final class Availability {
    private Availability() {
    }

    /**
     * Available stock is never stored; it is always computed.
     */
    public static int of(int quantityOnHand, int quantityReserved) {
        if (quantityOnHand < 0 || quantityReserved < 0) {
            throw new IllegalArgumentException("Quantities must be non-negative");
        }
        if (quantityReserved > quantityOnHand) {
            throw new IllegalArgumentException("Reserved cannot exceed on-hand");
        }
        return quantityOnHand - quantityReserved;
    }

    public static boolean canFulfill(int quantityOnHand, int quantityReserved, int requested) {
        return requested > 0 && of(quantityOnHand, quantityReserved) >= requested;
    }
}
