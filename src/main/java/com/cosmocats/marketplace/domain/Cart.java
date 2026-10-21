package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shopping cart aggregate. Holds products with quantities and knows how to
 * turn itself into an {@link Order}.
 */
public record Cart(Long id, String customerEmail, List<CartItem> items) {

    public Cart {
        Objects.requireNonNull(customerEmail, "Customer email must not be null");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Cart empty(Long id, String customerEmail) {
        return new Cart(id, customerEmail, List.of());
    }

    public Cart addProduct(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        List<CartItem> updated = new ArrayList<>();
        boolean merged = false;
        for (CartItem item : items) {
            if (item.product().id().equals(product.id())) {
                updated.add(item.withQuantity(item.quantity() + quantity));
                merged = true;
            } else {
                updated.add(item);
            }
        }
        if (!merged) {
            updated.add(new CartItem(product, quantity));
        }
        return new Cart(id, customerEmail, updated);
    }

    public Cart removeProduct(Long productId) {
        return new Cart(id, customerEmail, items.stream()
                .filter(item -> !item.product().id().equals(productId))
                .toList());
    }

    public BigDecimal total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Order checkout(Long orderId, Instant now) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        List<OrderItem> orderItems = items.stream()
                .map(item -> new OrderItem(
                        item.product().id(),
                        item.product().name(),
                        item.product().price(),
                        item.quantity()))
                .toList();
        return Order.place(orderId, customerEmail, orderItems, now);
    }
}
