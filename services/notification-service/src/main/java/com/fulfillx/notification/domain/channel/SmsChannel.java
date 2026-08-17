package com.fulfillx.notification.domain.channel;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public Channel type() {
        return Channel.SMS;
    }

    @Override
    public void send(Notification notification) {
        log.info(
                "SMS channel (v1 log-only) template={} recipient={} orderId={}",
                notification.getTemplate(),
                notification.getRecipient(),
                notification.getOrderId()
        );
        notification.markSent();
    }
}
