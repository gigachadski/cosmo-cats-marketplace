package com.cosmocats.marketplace.persistence;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.persistence.adapter.JpaCategoryRepositoryAdapter;
import com.cosmocats.marketplace.persistence.adapter.JpaProductRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresDataJpaTest
@Import({JpaCategoryRepositoryAdapter.class, JpaProductRepositoryAdapter.class})
class CategoryRepositoryIT {

    @Autowired
    private JpaCategoryRepositoryAdapter categories;

    @Autowired
    private JpaProductRepositoryAdapter products;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void crudRoundTrip() {
        Category saved = categories.save(new Category(null, "Space Food"));
        entityManager.clear();
        assertThat(categories.findById(saved.id())).contains(saved);

        assertThat(categories.update(new Category(saved.id(), "Cosmic Food")))
                .contains(new Category(saved.id(), "Cosmic Food"));
        entityManager.clear();
        assertThat(categories.findById(saved.id())).get().extracting(Category::name).isEqualTo("Cosmic Food");

        assertThat(categories.deleteById(saved.id())).isTrue();
        assertThat(categories.deleteById(saved.id())).isFalse();
        assertThat(categories.findById(saved.id())).isEmpty();
    }

    @Test
    void findAllIsOrderedByName() {
        categories.save(new Category(null, "Zero-G Toys"));
        categories.save(new Category(null, "Orbital Furniture"));
        categories.save(new Category(null, "Space Food"));

        assertThat(categories.findAll()).extracting(Category::name)
                .containsExactly("Orbital Furniture", "Space Food", "Zero-G Toys");
    }

    @Test
    void updateOfMissingCategoryIsEmpty() {
        assertThat(categories.update(new Category(999_999L, "Nothing"))).isEmpty();
    }

    @Test
    void newCategoryMustNotHaveAnId() {
        assertThatThrownBy(() -> categories.save(new Category(1L, "Space Food")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void categoryNameIsUnique() {
        categories.save(new Category(null, "Space Food"));

        assertThatThrownBy(() -> categories.save(new Category(null, "Space Food")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_category_name");
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        Category food = categories.save(new Category(null, "Space Food"));
        products.save(new Product(null, "Star Tuna Can", null, BigDecimal.TEN, food, "a@cosmocats.space"));

        assertThatThrownBy(() -> categories.deleteById(food.id()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_product_category");
    }
}
