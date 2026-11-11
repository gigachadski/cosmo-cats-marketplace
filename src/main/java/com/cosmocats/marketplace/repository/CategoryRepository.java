package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Category;

import java.util.List;
import java.util.Optional;

/** Domain port for categories. */
public interface CategoryRepository {

    Optional<Category> findById(Long id);

    /** All categories ordered by name. */
    List<Category> findAll();

    /** Saves a new category (its id must be {@code null}) and returns it with the generated id. */
    Category save(Category category);

    /** Renames the category with the same id; empty if it does not exist. */
    Optional<Category> update(Category category);

    /** Deletes the category and reports whether it existed. */
    boolean deleteById(Long id);
}
