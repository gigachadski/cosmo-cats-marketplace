package com.cosmocats.marketplace.domain;

import java.util.Objects;

/** Product category. {@code id} is {@code null} until the category is saved. */
public record Category(Long id, String name) {

    public Category {
        Objects.requireNonNull(name, "Category name must not be null");
    }

    public Category withId(Long newId) {
        return new Category(newId, name);
    }
}
