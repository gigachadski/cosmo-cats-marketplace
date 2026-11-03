package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.ProductSummaryDto;

import java.util.List;
import java.util.Optional;

/** Domain port for products; the database adapter lives in the persistence package. */
public interface ProductRepository {

    /** Saves a new product (its id must be {@code null}) and returns it with the generated id. */
    Product save(Product product);

    Optional<Product> findById(Long id);

    /** Returns one page of products ordered by id; {@code page} is zero-based. */
    List<Product> findPage(int page, int size);

    long count();

    boolean existsById(Long id);

    /** Replaces the product with the same id; empty if it does not exist (never creates one). */
    Optional<Product> update(Product product);

    /** Deletes the product and reports whether it existed. */
    boolean deleteById(Long id);

    /** Short view of the products of one category, ordered by name. */
    List<ProductSummaryDto> findSummariesByCategory(Long categoryId);
}
