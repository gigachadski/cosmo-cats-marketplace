package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root. The total is always derived from the items, never stored,
 * and status changes go through {@link OrderStatus} transition rules.
 */
public record Order(
        Long id,
        String customerEmail,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt) {

    public Order {
        Objects.requireNonNull(customerEmail, "Customer email must not be null");
        Objects.requireNonNull(status, "Order status must not be null");
        Objects.requireNonNull(createdAt, "Order creation time must not be null");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        items = List.copyOf(items);
    }

    public static Order place(Long id, String customerEmail, List<OrderItem> items, Instant now) {
        return new Order(id, customerEmail, items, OrderStatus.NEW, now);
    }

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Order pay() {
        return transitionTo(OrderStatus.PAID);
    }

    public Order ship() {
        return transitionTo(OrderStatus.SHIPPED);
    }

    public Order deliver() {
        return transitionTo(OrderStatus.DELIVERED);
    }

    public Order cancel() {
        return transitionTo(OrderStatus.CANCELLED);
    }

    private Order transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Cannot change order status from " + status + " to " + target);
        }
        return new Order(id, customerEmail, items, target, createdAt);
    }
}
