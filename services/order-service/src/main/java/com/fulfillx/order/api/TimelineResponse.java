package com.fulfillx.order.api;

import java.time.Instant;
import java.util.List;

public record TimelineResponse(List<TimelineEntry> entries) {
    public record TimelineEntry(
            Instant at,
            String type,
            String oldStatus,
            String newStatus,
            String reason
    ) {
    }
}
