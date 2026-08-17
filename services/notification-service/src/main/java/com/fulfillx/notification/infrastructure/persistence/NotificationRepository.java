package com.fulfillx.notification.infrastructure.persistence;

import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.Notification;
import com.fulfillx.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIgnoreCaseOrderByCreatedAtDesc(String recipient);

    Optional<Notification> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:orderId IS NULL OR n.orderId = :orderId)
              AND (:channel IS NULL OR n.channel = :channel)
              AND (:status IS NULL OR n.status = :status)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> search(
            @Param("orderId") UUID orderId,
            @Param("channel") Channel channel,
            @Param("status") NotificationStatus status,
            Pageable pageable
    );
}
