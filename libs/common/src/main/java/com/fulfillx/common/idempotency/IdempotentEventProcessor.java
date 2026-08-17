package com.fulfillx.common.idempotency;

import com.fulfillx.common.event.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Consumer;

@Service
public class IdempotentEventProcessor {
    private final ProcessedEventRepository processedEvents;

    public IdempotentEventProcessor(ProcessedEventRepository processedEvents) {
        this.processedEvents = processedEvents;
    }

    @Transactional
    public void process(DomainEvent event, Consumer<DomainEvent> handler) {
        if (processedEvents.existsById(event.eventId())) {
            return;
        }
        handler.accept(event);
        processedEvents.save(new ProcessedEvent(event.eventId(), event.eventType(), Instant.now()));
    }
}
