package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Category;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<Long, Category> storage = Map.of(
            1L, new Category(1L, "Space Food"),
            2L, new Category(2L, "Zero-G Toys"),
            3L, new Category(3L, "Orbital Furniture"));

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }
}
