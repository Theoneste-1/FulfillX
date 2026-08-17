package com.fulfillx.shipment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTransitionsTest {
    @Test
    void happyPathIsAllowed() {
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.CREATED, Shipment.Status.ASSIGNED)).isTrue();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.ASSIGNED, Shipment.Status.PICKED_UP)).isTrue();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.PICKED_UP, Shipment.Status.IN_TRANSIT)).isTrue();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.IN_TRANSIT, Shipment.Status.OUT_FOR_DELIVERY)).isTrue();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.OUT_FOR_DELIVERY, Shipment.Status.DELIVERED)).isTrue();
    }

    @Test
    void rejectsIllegalJumps() {
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.CREATED, Shipment.Status.DELIVERED)).isFalse();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.DELIVERED, Shipment.Status.IN_TRANSIT)).isFalse();
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.FAILED, Shipment.Status.ASSIGNED)).isFalse();
    }

    @Test
    void delayedCanResumeToInTransit() {
        assertThat(ShipmentTransitions.canTransition(Shipment.Status.DELAYED, Shipment.Status.IN_TRANSIT)).isTrue();
        assertThat(ShipmentTransitions.nextAutoProgress(Shipment.Status.CREATED)).isEqualTo(Shipment.Status.ASSIGNED);
        assertThat(ShipmentTransitions.nextAutoProgress(Shipment.Status.OUT_FOR_DELIVERY)).isEqualTo(Shipment.Status.DELIVERED);
        assertThat(ShipmentTransitions.nextAutoProgress(Shipment.Status.DELIVERED)).isNull();
    }

    @Test
    void trackingNumberMatchesContract() {
        String tracking = TrackingNumbers.fulfillxLogistics();
        assertThat(tracking).matches("FX-FXL-[A-Z0-9]{12}");
    }
}
