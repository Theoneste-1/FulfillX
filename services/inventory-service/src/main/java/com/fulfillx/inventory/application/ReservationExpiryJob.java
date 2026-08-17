package com.fulfillx.inventory.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryJob {
    private final InventoryService inventory;

    public ReservationExpiryJob(InventoryService inventory) {
        this.inventory = inventory;
    }

    @Scheduled(fixedDelay = 30_000)
    public void expire() {
        inventory.expireDue();
    }
}
