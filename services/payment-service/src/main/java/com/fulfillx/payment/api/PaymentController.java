package com.fulfillx.payment.api;

import com.fulfillx.payment.application.PaymentService;
import com.fulfillx.security.FulfillxPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse getByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        return paymentService.getByOrderId(orderId, principal);
    }
}
