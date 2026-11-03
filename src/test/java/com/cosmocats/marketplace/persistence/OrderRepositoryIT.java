package com.cosmocats.marketplace.persistence;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.persistence.adapter.JpaCategoryRepositoryAdapter;
import com.cosmocats.marketplace.persistence.adapter.JpaOrderRepositoryAdapter;
import com.cosmocats.marketplace.persistence.adapter.JpaProductRepositoryAdapter;
import com.cosmocats.marketplace.persistence.entity.CustomerEntity;
import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.persistence.repository.CustomerJpaRepository;
import com.cosmocats.marketplace.persistence.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresDataJpaTest
@Import({JpaOrderRepositoryAdapter.class, JpaProductRepositoryAdapter.class, JpaCategoryRepositoryAdapter.class})
class OrderRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-11-01T10:15:30Z");
    private static final EnumSet<OrderStatus> PURCHASED = EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    @Autowired
    private JpaOrderRepositoryAdapter orders;

    @Autowired
    private JpaProductRepositoryAdapter products;

    @Autowired
    private JpaCategoryRepositoryAdapter categories;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private CustomerJpaRepository customers;

    @Autowired
    private TestEntityManager entityManager;

    private Product tuna;
    private Product catnip;

    @BeforeEach
    void setUp() {
        Category food = categories.save(new Category(null, "Space Food"));
        tuna = products.save(new Product(null, "Star Tuna Can", null, new BigDecimal("9.99"), food, "a@cosmocats.space"));
        catnip = products.save(new Product(null, "Nebula Catnip", null, new BigDecimal("12.00"), food, "a@cosmocats.space"));
    }

    private Order newOrder(String email, Product product, int quantity) {
        return Order.place(UUID.randomUUID(), email,
                List.of(new OrderItem(product.id(), product.name(), product.price(), quantity)), NOW);
    }

    @Test
    void savedOrderIsFoundByItsNaturalId() {
        Order saved = orders.save(newOrder("cat@cosmocats.space", tuna, 2));
        entityManager.clear();

        Order loaded = orders.findByOrderNumber(saved.orderNumber()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.customerEmail()).isEqualTo("cat@cosmocats.space");
        assertThat(loaded.status()).isEqualTo(OrderStatus.NEW);
        assertThat(loaded.createdAt()).isEqualTo(NOW.truncatedTo(ChronoUnit.MICROS));
        assertThat(loaded.items()).containsExactly(new OrderItem(tuna.id(), "Star Tuna Can", new BigDecimal("9.99"), 2));
        assertThat(loaded.total()).isEqualByComparingTo("19.98");
    }

    @Test
    void naturalIdLookupUsesOrderNumberNotSurrogateKey() {
        Order saved = orders.save(newOrder("cat@cosmocats.space", tuna, 1));
        entityManager.clear();

        OrderEntity entity = orderJpaRepository.findByNaturalId(saved.orderNumber()).orElseThrow();

        assertThat(entity.getId()).isEqualTo(saved.id());
        assertThat(orderJpaRepository.findByNaturalId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void unitPriceIsSnapshotOfPurchaseTime() {
        Order saved = orders.save(newOrder("cat@cosmocats.space", tuna, 1));
        products.update(new Product(tuna.id(), tuna.name(), null, new BigDecimal("99.99"), tuna.category(), tuna.sellerEmail()));
        entityManager.clear();

        assertThat(orders.findByOrderNumber(saved.orderNumber()).orElseThrow().items().getFirst().unitPrice())
                .isEqualByComparingTo("9.99");
    }

    @Test
    void customerIsCreatedOnceAndReusedByEmail() {
        orders.save(newOrder("cat@cosmocats.space", tuna, 1));
        orders.save(newOrder("cat@cosmocats.space", catnip, 1));
        orders.save(newOrder("dog@cosmocats.space", catnip, 1));

        assertThat(customers.count()).isEqualTo(2);
        assertThat(customers.findByEmail("cat@cosmocats.space")).isPresent();
    }

    @Test
    void customerEmailIsUnique() {
        customers.saveAndFlush(new CustomerEntity("cat@cosmocats.space"));

        assertThatThrownBy(() -> customers.saveAndFlush(new CustomerEntity("cat@cosmocats.space")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_customer_email");
    }

    @Test
    void orderNumberIsUnique() {
        Order first = orders.save(newOrder("cat@cosmocats.space", tuna, 1));
        Order duplicate = Order.place(first.orderNumber(), "dog@cosmocats.space", first.items(), NOW);

        assertThatThrownBy(() -> orders.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void newOrderMustNotHaveAnId() {
        Order withId = new Order(5L, UUID.randomUUID(), "cat@cosmocats.space",
                newOrder("cat@cosmocats.space", tuna, 1).items(), OrderStatus.NEW, NOW);

        assertThatThrownBy(() -> orders.save(withId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusUpdateIsPersisted() {
        Order saved = orders.save(newOrder("cat@cosmocats.space", tuna, 1));

        assertThat(orders.updateStatus(saved.orderNumber(), OrderStatus.PAID)).get()
                .extracting(Order::status).isEqualTo(OrderStatus.PAID);
        entityManager.clear();

        assertThat(orders.findByOrderNumber(saved.orderNumber()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
        assertThat(orders.updateStatus(UUID.randomUUID(), OrderStatus.PAID)).isEmpty();
    }

    @Test
    void deleteRemovesOrderWithItems() {
        Order saved = orders.save(newOrder("cat@cosmocats.space", tuna, 1));

        assertThat(orders.deleteByOrderNumber(saved.orderNumber())).isTrue();
        assertThat(orders.deleteByOrderNumber(saved.orderNumber())).isFalse();
        assertThat(orders.findByOrderNumber(saved.orderNumber())).isEmpty();
        assertThat(entityManager.getEntityManager()
                .createQuery("select count(i) from OrderItemEntity i", Long.class).getSingleResult()).isZero();
    }

    @Test
    void orderedProductCannotBeDeleted() {
        orders.save(newOrder("cat@cosmocats.space", tuna, 1));

        assertThatThrownBy(() -> products.deleteById(tuna.id()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_order_item_product");
    }

    @Test
    void topSellingReportSumsQuantitiesOfPurchasedOrdersOnly() {
        Order paidTuna = orders.save(newOrder("a@cosmocats.space", tuna, 2));
        Order paidCatnip = orders.save(newOrder("b@cosmocats.space", catnip, 5));
        Order shippedTuna = orders.save(newOrder("a@cosmocats.space", tuna, 1));
        orders.save(newOrder("c@cosmocats.space", tuna, 100)); // NEW: not a purchase
        orders.updateStatus(paidTuna.orderNumber(), OrderStatus.PAID);
        orders.updateStatus(paidCatnip.orderNumber(), OrderStatus.PAID);
        orders.updateStatus(shippedTuna.orderNumber(), OrderStatus.SHIPPED);
        entityManager.clear();

        List<ProductSalesDto> report = orders.findTopSellingProducts(PURCHASED, 10);

        assertThat(report).hasSize(2);
        assertThat(report.get(0)).satisfies(row -> {
            assertThat(row.productName()).isEqualTo("Nebula Catnip");
            assertThat(row.totalQuantity()).isEqualTo(5);
            assertThat(row.totalRevenue()).isEqualByComparingTo("60.00");
        });
        assertThat(report.get(1)).satisfies(row -> {
            assertThat(row.productId()).isEqualTo(tuna.id());
            assertThat(row.totalQuantity()).isEqualTo(3);
            assertThat(row.totalRevenue()).isEqualByComparingTo("29.97");
        });
    }

    @Test
    void topSellingReportRespectsLimitAndTieBreaksByName() {
        orders.updateStatus(orders.save(newOrder("a@cosmocats.space", tuna, 1)).orderNumber(), OrderStatus.PAID);
        orders.updateStatus(orders.save(newOrder("a@cosmocats.space", catnip, 1)).orderNumber(), OrderStatus.PAID);

        assertThat(orders.findTopSellingProducts(PURCHASED, 1))
                .extracting(ProductSalesDto::productName).containsExactly("Nebula Catnip");
        assertThat(orders.findTopSellingProducts(EnumSet.of(OrderStatus.CANCELLED), 10)).isEmpty();
    }
}
