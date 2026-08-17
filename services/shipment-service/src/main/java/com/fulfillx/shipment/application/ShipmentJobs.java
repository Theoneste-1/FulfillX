package com.fulfillx.shipment.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShipmentJobs {
    private final ShipmentService shipments;
    private final boolean autoProgress;

    public ShipmentJobs(ShipmentService shipments, @Value("${fulfillx.demo.auto-progress-shipments:true}") boolean autoProgress) {
        this.shipments = shipments;
        this.autoProgress = autoProgress;
    }

    @Scheduled(fixedDelay = 8000)
    public void progress() {
        if (autoProgress) {
            shipments.autoProgress();
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void delays() {
        shipments.detectDelays();
    }
}
