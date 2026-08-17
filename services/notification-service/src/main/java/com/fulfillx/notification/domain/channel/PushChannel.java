package com.fulfillx.notification.domain.channel;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    @Override
    public Channel type() {
        return Channel.PUSH;
    }

    @Override
    public void send(Notification notification) {
        log.info(
                "Push channel (v1 log-only) template={} recipient={} orderId={}",
                notification.getTemplate(),
                notification.getRecipient(),
                notification.getOrderId()
        );
        notification.markSent();
    }
}
