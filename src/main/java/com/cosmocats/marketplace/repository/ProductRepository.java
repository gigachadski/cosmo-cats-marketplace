package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    /** Saves the product, assigning a new id when the product has none. */
    Product save(Product product);

    Optional<Product> findById(Long id);

    /** Returns products ordered by id, skipping {@code offset} and returning at most {@code limit}. */
    List<Product> findAll(long offset, int limit);

    long count();

    boolean existsById(Long id);

    void deleteById(Long id);
}
