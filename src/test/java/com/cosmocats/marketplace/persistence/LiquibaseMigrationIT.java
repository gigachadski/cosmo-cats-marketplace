package com.cosmocats.marketplace.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the schema that Liquibase built: sequences, constraints, indexes and the changelog itself. */
@PostgresDataJpaTest
class LiquibaseMigrationIT {

    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private List<String> strings(String sql) {
        return (List<String>) entityManager.createNativeQuery(sql).getResultList();
    }

    @Test
    void allChangeSetsExceptDemoSeedAreApplied() {
        assertThat(strings("SELECT id FROM databasechangelog ORDER BY orderexecuted")).containsExactly(
                "001-create-sequences",
                "002-create-tables",
                "003-unique-product-name-in-category",
                "004-index-product-name",
                "005-unique-customer-email");
    }

    @Test
    void idSequencesExist() {
        assertThat(strings("SELECT sequence_name::text FROM information_schema.sequences"))
                .containsExactlyInAnyOrder("category_seq", "product_seq", "customer_seq", "orders_seq", "order_item_seq");
    }

    @Test
    void uniqueConstraintsExist() {
        assertThat(strings("SELECT conname::text FROM pg_constraint WHERE contype = 'u'"))
                .contains("uq_product_category_name", "uq_customer_email", "uq_category_name", "uq_orders_order_number");
    }

    @Test
    void searchAndForeignKeyIndexesExist() {
        assertThat(strings("SELECT indexname::text FROM pg_indexes WHERE schemaname = 'public'"))
                .contains("idx_product_name", "idx_product_category_id", "idx_orders_customer_id",
                        "idx_order_item_order_id", "idx_order_item_product_id");
    }

    @Test
    void productNameIndexCoversNameColumn() {
        assertThat(strings("SELECT indexdef::text FROM pg_indexes WHERE indexname = 'idx_product_name'"))
                .singleElement().asString().contains("(name)");
    }

    @Test
    void checkConstraintsGuardBusinessRules() {
        assertThat(strings("SELECT conname::text FROM pg_constraint WHERE contype = 'c'"))
                .contains("ck_product_price_positive", "ck_order_item_quantity_positive", "ck_orders_status");
    }
}
