package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRepositoryTest {

    private final CategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private InMemoryProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository(categoryRepository);
    }

    private Product newProduct(String name) {
        return new Product(null, name, null, BigDecimal.TEN, categoryRepository.findById(1L).orElseThrow(), "a@b.space");
    }

    @Test
    void categoriesAreSeeded() {
        assertThat(categoryRepository.findById(2L)).contains(new Category(2L, "Zero-G Toys"));
        assertThat(categoryRepository.findById(42L)).isEmpty();
    }

    @Test
    void productsAreSeeded() {
        assertThat(productRepository.count()).isEqualTo(5);
        assertThat(productRepository.findById(1L)).get().extracting(Product::name).isEqualTo("Star Tuna Can");
    }

    @Test
    void saveAssignsNextIdToNewProduct() {
        Product saved = productRepository.save(newProduct("Galaxy Milk"));

        assertThat(saved.id()).isEqualTo(6L);
        assertThat(productRepository.findById(6L)).contains(saved);
    }

    @Test
    void saveWithIdReplacesExistingProduct() {
        productRepository.save(newProduct("Star Renamed").withId(1L));

        assertThat(productRepository.count()).isEqualTo(5);
        assertThat(productRepository.findById(1L)).get().extracting(Product::name).isEqualTo("Star Renamed");
    }

    @Test
    void idsAreNotReusedAfterDelete() {
        productRepository.deleteById(5L);

        assertThat(productRepository.existsById(5L)).isFalse();
        assertThat(productRepository.save(newProduct("Comet Snack")).id()).isEqualTo(6L);
    }

    @Test
    void findAllPagesInIdOrder() {
        assertThat(productRepository.findAll(0, 2)).extracting(Product::id).containsExactly(1L, 2L);
        assertThat(productRepository.findAll(4, 2)).extracting(Product::id).containsExactly(5L);
        assertThat(productRepository.findAll(10, 2)).isEmpty();
    }
}
