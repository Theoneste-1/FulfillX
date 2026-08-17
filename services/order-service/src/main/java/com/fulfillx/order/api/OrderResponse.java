package com.fulfillx.order.api;

import com.fulfillx.order.domain.Order;
import com.fulfillx.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        ShippingAddressResponse shippingAddress,
        UUID warehouseId,
        UUID paymentId,
        String cancellationReason,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                new ShippingAddressResponse(
                        order.getShippingLine1(),
                        order.getShippingLine2(),
                        order.getShippingCity(),
                        order.getShippingRegion(),
                        order.getShippingPostal(),
                        order.getShippingCountry()
                ),
                order.getWarehouseId(),
                order.getPaymentId(),
                order.getCancellationReason(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public record ShippingAddressResponse(
            String line1,
            String line2,
            String city,
            String region,
            String postalCode,
            String country
    ) {
    }

    public record OrderItemResponse(
            UUID id,
            UUID productId,
            String sku,
            String name,
            int quantity,
            BigDecimal unitPrice
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getSku(),
                    item.getName(),
                    item.getQuantity(),
                    item.getUnitPrice()
            );
        }
    }
}
