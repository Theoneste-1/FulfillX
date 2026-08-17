package com.fulfillx.notification.application;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import com.fulfillx.notification.domain.NotificationTemplates;
import com.fulfillx.notification.domain.channel.NotificationChannel;
import com.fulfillx.notification.infrastructure.persistence.NotificationRepository;
import com.fulfillx.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationDispatchService {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final Map<Channel, NotificationChannel> channels;
    private final NotificationRepository notifications;
    private final OutboxWriter outboxWriter;

    public NotificationDispatchService(
            List<NotificationChannel> channelList,
            NotificationRepository notifications,
            OutboxWriter outboxWriter
    ) {
        this.channels = new EnumMap<>(Channel.class);
        for (NotificationChannel channel : channelList) {
            this.channels.put(channel.type(), channel);
        }
        this.notifications = notifications;
        this.outboxWriter = outboxWriter;
    }

    public void dispatch(DomainEvent event) {
        String template = NotificationTemplates.forEventType(event.eventType());
        if (template == null) {
            return;
        }
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        UUID orderId = uuid(payload, "orderId");
        String recipient = resolveRecipient(payload, orderId);
        if (recipient == null) {
            log.info("Skipping {} — no customerEmail in payload (orderId={})", event.eventType(), orderId);
            return;
        }
        Channel channel = Channel.EMAIL;
        Notification notification = Notification.create(
                orderId,
                uuid(payload, "customerId"),
                channel,
                template,
                recipient,
                NotificationTemplates.subject(template),
                NotificationTemplates.body(template, payload)
        );
        NotificationChannel sender = channels.get(channel);
        if (sender == null) {
            log.warn("No channel implementation for {}", channel);
            return;
        }
        sender.send(notification);
        outboxWriter.enqueue(
                EventTypes.NOTIFICATION_DISPATCHED,
                "Notification",
                notification.getId().toString(),
                Map.of(
                        "notificationId", notification.getId().toString(),
                        "orderId", orderId == null ? "" : orderId.toString(),
                        "channel", notification.getChannel().name(),
                        "template", notification.getTemplate(),
                        "recipient", notification.getRecipient(),
                        "status", notification.getStatus().name()
                ),
                event.eventId() == null ? null : event.eventId().toString()
        );
    }

    private String resolveRecipient(Map<String, Object> payload, UUID orderId) {
        String email = text(payload, "customerEmail");
        if (email == null) {
            email = text(payload, "email");
        }
        if (email == null && orderId != null) {
            email = notifications.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                    .map(Notification::getRecipient)
                    .orElse(null);
        }
        return email;
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static UUID uuid(Map<String, Object> payload, String key) {
        String text = text(payload, key);
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
