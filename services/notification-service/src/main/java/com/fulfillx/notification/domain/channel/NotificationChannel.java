package com.fulfillx.notification.domain.channel;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;

public interface NotificationChannel {
    Channel type();

    void send(Notification notification);
}
