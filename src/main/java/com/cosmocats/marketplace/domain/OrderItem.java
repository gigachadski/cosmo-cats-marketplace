package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Snapshot of a product at the moment the order was placed, so later price
 * changes of the product do not affect existing orders.
 */
public record OrderItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {

    public OrderItem {
        Objects.requireNonNull(productId, "Product id must not be null");
        Objects.requireNonNull(productName, "Product name must not be null");
        Objects.requireNonNull(unitPrice, "Unit price must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
