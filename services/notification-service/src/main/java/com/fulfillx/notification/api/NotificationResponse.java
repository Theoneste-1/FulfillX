package com.fulfillx.notification.api;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import com.fulfillx.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        Channel channel,
        String template,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrderId(),
                notification.getUserId(),
                notification.getChannel(),
                notification.getTemplate(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
