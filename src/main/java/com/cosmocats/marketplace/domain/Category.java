package com.cosmocats.marketplace.domain;

import java.util.Objects;

public record Category(Long id, String name) {

    public Category {
        Objects.requireNonNull(id, "Category id must not be null");
        Objects.requireNonNull(name, "Category name must not be null");
    }
}
