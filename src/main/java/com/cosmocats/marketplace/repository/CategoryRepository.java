package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Category;

import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long id);
}
