package com.fulfillx.outbox;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.json.Jsons;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class OutboxWriter {
    private final OutboxEventRepository repository;

    public OutboxWriter(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DomainEvent enqueue(
            String topicEventType,
            String aggregateType,
            String aggregateId,
            Map<String, Object> payload,
            String causationId
    ) {
        String correlationId = MDC.get("correlationId");
        DomainEvent event = DomainEvent.of(
                topicEventType,
                aggregateType,
                aggregateId,
                correlationId,
                causationId,
                payload
        );
        repository.save(OutboxEvent.create(
                aggregateType,
                aggregateId,
                topicEventType,
                Jsons.write(event),
                correlationId,
                causationId
        ));
        return event;
    }
}
