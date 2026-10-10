package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CartItem(Product product, int quantity) {

    public CartItem {
        Objects.requireNonNull(product, "Product must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(product, newQuantity);
    }

    public BigDecimal subtotal() {
        return product.price().multiply(BigDecimal.valueOf(quantity));
    }
}
