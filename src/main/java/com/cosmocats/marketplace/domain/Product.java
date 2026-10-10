package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product aggregate root. Immutable: every change produces a new instance,
 * and invariants are checked once in the canonical constructor.
 */
public record Product(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Category category,
        String sellerEmail) {

    public Product {
        Objects.requireNonNull(name, "Product name must not be null");
        Objects.requireNonNull(price, "Product price must not be null");
        Objects.requireNonNull(category, "Product category must not be null");
        Objects.requireNonNull(sellerEmail, "Seller email must not be null");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
    }

    public Product withId(Long newId) {
        return new Product(newId, name, description, price, category, sellerEmail);
    }
}
