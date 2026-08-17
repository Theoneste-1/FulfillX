package com.fulfillx.notification.domain.channel;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import com.fulfillx.notification.infrastructure.persistence.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);
    private final NotificationRepository notifications;

    public EmailChannel(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public Channel type() {
        return Channel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        notification.markSent();
        notifications.save(notification);
        log.info(
                "Email sent template={} recipient={} orderId={} subject={}",
                notification.getTemplate(),
                notification.getRecipient(),
                notification.getOrderId(),
                notification.getSubject()
        );
    }
}
