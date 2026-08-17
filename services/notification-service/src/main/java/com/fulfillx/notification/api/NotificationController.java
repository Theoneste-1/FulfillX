package com.fulfillx.notification.api;

import com.fulfillx.common.api.PageResponse;
import com.fulfillx.notification.application.NotificationQueryService;
import com.fulfillx.notification.domain.Channel;
import com.fulfillx.notification.domain.NotificationStatus;
import com.fulfillx.security.FulfillxPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationQueryService queryService;

    public NotificationController(NotificationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable
    ) {
        return queryService.search(orderId, channel, status, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<NotificationResponse> me(@AuthenticationPrincipal FulfillxPrincipal principal) {
        if (principal == null || principal.email() == null) {
            return List.of();
        }
        return queryService.forRecipient(principal.email());
    }
}
