package com.fulfillx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DomainEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String aggregateType,
        String aggregateId,
        int schemaVersion,
        String correlationId,
        String causationId,
        Map<String, Object> payload
) {
    public static DomainEvent of(
            String eventType,
            String aggregateType,
            String aggregateId,
            String correlationId,
            String causationId,
            Map<String, Object> payload
    ) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                aggregateType,
                aggregateId,
                1,
                correlationId,
                causationId,
                payload
        );
    }
}
