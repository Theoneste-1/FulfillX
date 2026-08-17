package com.fulfillx.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty List<@Valid OrderItemRequest> items,
        @Valid @NotNull ShippingAddressRequest shippingAddress,
        @Size(min = 3, max = 3) String currency,
        UUID customerId,
        SimulationRequest simulation
) {
    public record OrderItemRequest(
            @NotNull UUID productId,
            @Min(1) int quantity
    ) {
    }

    public record ShippingAddressRequest(
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            String region,
            String postalCode,
            @NotBlank @Size(min = 2, max = 2) String country
    ) {
    }

    public record SimulationRequest(Boolean paymentFail) {
        public boolean enabled() {
            return Boolean.TRUE.equals(paymentFail);
        }
    }
}
