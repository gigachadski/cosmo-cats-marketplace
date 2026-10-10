package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock storage that keeps products in memory. A sorted concurrent map gives
 * thread safety and a stable order for pagination; ids come from a sequence,
 * so deleting a product never causes an id to be reused.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentNavigableMap<Long, Product> storage = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public InMemoryProductRepository(CategoryRepository categoryRepository) {
        Category food = categoryRepository.findById(1L).orElseThrow();
        Category toys = categoryRepository.findById(2L).orElseThrow();
        Category furniture = categoryRepository.findById(3L).orElseThrow();

        save(new Product(null, "Star Tuna Can", "Tuna caught near Sirius",
                new BigDecimal("9.99"), food, "tuna@cosmocats.space"));
        save(new Product(null, "Galaxy Yarn Ball", "Glows in the dark of deep space",
                new BigDecimal("4.50"), toys, "yarn@cosmocats.space"));
        save(new Product(null, "Comet Scratching Post", "Fast as a comet, sturdy as a planet",
                new BigDecimal("49.90"), furniture, "post@cosmocats.space"));
        save(new Product(null, "Nebula Catnip", "Harvested in the Crab Nebula",
                new BigDecimal("12.00"), food, "catnip@cosmocats.space"));
        save(new Product(null, "Moon Laser Pointer", "The red dot visible from orbit",
                new BigDecimal("19.95"), toys, "laser@cosmocats.space"));
    }

    @Override
    public Product save(Product product) {
        Product toStore = product.id() == null ? product.withId(sequence.incrementAndGet()) : product;
        storage.put(toStore.id(), toStore);
        return toStore;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Product> findAll(long offset, int limit) {
        return storage.values().stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }
}
