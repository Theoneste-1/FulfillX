package com.fulfillx.order.domain;

public record ShippingAddress(
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String country
) {
}
