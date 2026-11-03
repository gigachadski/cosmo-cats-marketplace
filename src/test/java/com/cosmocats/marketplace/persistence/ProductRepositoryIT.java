package com.cosmocats.marketplace.persistence;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.ProductSummaryDto;
import com.cosmocats.marketplace.persistence.adapter.JpaCategoryRepositoryAdapter;
import com.cosmocats.marketplace.persistence.adapter.JpaProductRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresDataJpaTest
@Import({JpaProductRepositoryAdapter.class, JpaCategoryRepositoryAdapter.class})
class ProductRepositoryIT {

    @Autowired
    private JpaProductRepositoryAdapter products;

    @Autowired
    private JpaCategoryRepositoryAdapter categories;

    @Autowired
    private TestEntityManager entityManager;

    private Category food;
    private Category toys;

    @BeforeEach
    void setUp() {
        food = categories.save(new Category(null, "Space Food"));
        toys = categories.save(new Category(null, "Zero-G Toys"));
    }

    private Product newProduct(String name, String price, Category category) {
        return new Product(null, name, "desc", new BigDecimal(price), category, "seller@cosmocats.space");
    }

    @Test
    void saveAssignsIdFromSequenceAndPersistsAllColumns() {
        Product saved = products.save(newProduct("Star Tuna Can", "9.99", food));
        entityManager.clear();

        assertThat(saved.id()).isNotNull();
        assertThat(products.findById(saved.id())).contains(saved);
        assertThat(products.existsById(saved.id())).isTrue();
    }

    @Test
    void newProductMustNotHaveAnId() {
        assertThatThrownBy(() -> products.save(newProduct("Star", "1", food).withId(5L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findPageReturnsProductsOrderedById() {
        Product first = products.save(newProduct("Star A", "1", food));
        Product second = products.save(newProduct("Star B", "2", toys));
        Product third = products.save(newProduct("Star C", "3", food));
        entityManager.clear();

        assertThat(products.findPage(0, 2)).extracting(Product::id).containsExactly(first.id(), second.id());
        assertThat(products.findPage(1, 2)).extracting(Product::id).containsExactly(third.id());
        assertThat(products.findPage(5, 2)).isEmpty();
        assertThat(products.count()).isEqualTo(3);
    }

    @Test
    void updateReplacesFieldsIncludingCategory() {
        Product saved = products.save(newProduct("Star Tuna Can", "9.99", food));

        Product changed = new Product(saved.id(), "Star Tuna XL", null, new BigDecimal("19.99"), toys, "new@cosmocats.space");
        assertThat(products.update(changed)).contains(changed);
        entityManager.clear();

        assertThat(products.findById(saved.id())).contains(changed);
    }

    @Test
    void updateNeverCreatesMissingProduct() {
        assertThat(products.update(newProduct("Ghost Star", "1", food).withId(999_999L))).isEmpty();
        assertThat(products.existsById(999_999L)).isFalse();
    }

    @Test
    void deleteReportsWhetherProductExisted() {
        Product saved = products.save(newProduct("Star Tuna Can", "9.99", food));

        assertThat(products.deleteById(saved.id())).isTrue();
        assertThat(products.deleteById(saved.id())).isFalse();
        assertThat(products.findById(saved.id())).isEmpty();
    }

    @Test
    void productNameIsUniqueWithinCategory() {
        products.save(newProduct("Star Tuna Can", "9.99", food));

        assertThatThrownBy(() -> products.save(newProduct("Star Tuna Can", "5.00", food)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_product_category_name");
    }

    @Test
    void sameNameIsAllowedInAnotherCategory() {
        products.save(newProduct("Star Tuna Can", "9.99", food));

        assertThat(products.save(newProduct("Star Tuna Can", "9.99", toys)).id()).isNotNull();
    }

    @Test
    void summariesOfCategoryAreOrderedByName() {
        Product tuna = products.save(newProduct("Star Tuna Can", "9.99", food));
        Product catnip = products.save(newProduct("Nebula Catnip", "12.00", food));
        products.save(newProduct("Galaxy Yarn", "4.50", toys));

        assertThat(products.findSummariesByCategory(food.id())).containsExactly(
                new ProductSummaryDto(catnip.id(), "Nebula Catnip", new BigDecimal("12.00")),
                new ProductSummaryDto(tuna.id(), "Star Tuna Can", new BigDecimal("9.99")));
    }
}
