package com.fulfillx.notification.application;

import com.fulfillx.common.api.PageResponse;
import com.fulfillx.notification.api.NotificationResponse;
import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.NotificationStatus;
import com.fulfillx.notification.infrastructure.persistence.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationQueryService {
    private final NotificationRepository notifications;

    public NotificationQueryService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> search(UUID orderId, Channel channel, NotificationStatus status, Pageable pageable) {
        return PageResponse.of(
                notifications.search(orderId, channel, status, pageable).map(NotificationResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> forRecipient(String email) {
        return notifications.findByRecipientIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
