package com.fulfillx.order.domain;

import com.fulfillx.order.application.OrderStateMachine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "shipping_line1", nullable = false, length = 200)
    private String shippingLine1;

    @Column(name = "shipping_line2", length = 200)
    private String shippingLine2;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String shippingCity;

    @Column(name = "shipping_region", length = 100)
    private String shippingRegion;

    @Column(name = "shipping_postal", length = 32)
    private String shippingPostal;

    @Column(name = "shipping_country", nullable = false, length = 2)
    private String shippingCountry;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    protected Order() {
    }

    public static Order create(
            String orderNumber,
            UUID customerId,
            String customerEmail,
            BigDecimal totalAmount,
            String currency,
            String shippingLine1,
            String shippingLine2,
            String shippingCity,
            String shippingRegion,
            String shippingPostal,
            String shippingCountry
    ) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.orderNumber = orderNumber;
        order.customerId = customerId;
        order.customerEmail = customerEmail;
        order.status = OrderStatus.CREATED;
        order.totalAmount = totalAmount;
        order.currency = currency;
        order.shippingLine1 = shippingLine1;
        order.shippingLine2 = shippingLine2;
        order.shippingCity = shippingCity;
        order.shippingRegion = shippingRegion;
        order.shippingPostal = shippingPostal;
        order.shippingCountry = shippingCountry;
        Instant now = Instant.now();
        order.createdAt = now;
        order.updatedAt = now;
        order.statusHistory.add(OrderStatusHistory.record(order, null, OrderStatus.CREATED, "Order created"));
        return order;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void transitionTo(OrderStatus newStatus, String reason) {
        OrderStateMachine.require(this.status, newStatus);
        statusHistory.add(OrderStatusHistory.record(this, this.status, newStatus, reason));
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void assignPayment(UUID paymentId) {
        this.paymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public void assignWarehouse(UUID warehouseId) {
        this.warehouseId = warehouseId;
        this.updatedAt = Instant.now();
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getShippingLine1() {
        return shippingLine1;
    }

    public String getShippingLine2() {
        return shippingLine2;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingRegion() {
        return shippingRegion;
    }

    public String getShippingPostal() {
        return shippingPostal;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }
}
