package com.cosmocats.marketplace.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelTest {

    private static final Category FOOD = new Category(1L, "Space Food");
    private static final Product TUNA = new Product(1L, "Star Tuna", null, new BigDecimal("0.10"), FOOD, "a@b.space");
    private static final Product CATNIP = new Product(2L, "Nebula Catnip", null, new BigDecimal("0.20"), FOOD, "a@b.space");
    private static final Instant NOW = Instant.parse("2026-10-10T10:00:00Z");
    private static final String CUSTOMER = "cat@cosmocats.space";

    @Nested
    class ProductInvariants {

        @Test
        void withIdKeepsAllOtherFields() {
            Product saved = new Product(null, "Star", "d", BigDecimal.ONE, FOOD, "a@b.space").withId(5L);

            assertThat(saved).isEqualTo(new Product(5L, "Star", "d", BigDecimal.ONE, FOOD, "a@b.space"));
        }

        @Test
        void rejectsNonPositivePrice() {
            assertThatThrownBy(() -> new Product(null, "Star", null, BigDecimal.ZERO, FOOD, "a@b.space"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Product(null, "Star", null, new BigDecimal("-1"), FOOD, "a@b.space"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsMissingRequiredFields() {
            assertThatThrownBy(() -> new Product(null, null, null, BigDecimal.ONE, FOOD, "a@b.space"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product(null, "Star", null, null, FOOD, "a@b.space"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product(null, "Star", null, BigDecimal.ONE, null, "a@b.space"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product(null, "Star", null, BigDecimal.ONE, FOOD, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void categoryRequiresIdAndName() {
            assertThatThrownBy(() -> new Category(null, "Food")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Category(1L, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class CartBehaviour {

        @Test
        void totalIsExactForDecimalPrices() {
            Cart cart = Cart.empty(1L, CUSTOMER).addProduct(TUNA, 1).addProduct(CATNIP, 1);

            // with Double this would be 0.30000000000000004
            assertThat(cart.total()).isEqualByComparingTo("0.30");
        }

        @Test
        void addingSameProductMergesQuantity() {
            Cart cart = Cart.empty(1L, CUSTOMER).addProduct(TUNA, 1).addProduct(CATNIP, 1).addProduct(TUNA, 2);

            assertThat(cart.items()).hasSize(2);
            assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
            assertThat(cart.total()).isEqualByComparingTo("0.50");
        }

        @Test
        void removeProductDropsOnlyThatProduct() {
            Cart cart = Cart.empty(1L, CUSTOMER).addProduct(TUNA, 1).addProduct(CATNIP, 1).removeProduct(1L);

            assertThat(cart.items()).extracting(item -> item.product().id()).containsExactly(2L);
        }

        @Test
        void cartIsImmutable() {
            Cart empty = Cart.empty(1L, CUSTOMER);
            Cart withTuna = empty.addProduct(TUNA, 1);

            assertThat(empty.isEmpty()).isTrue();
            assertThat(withTuna.isEmpty()).isFalse();
            assertThatThrownBy(() -> withTuna.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void nullItemsMeanEmptyCart() {
            assertThat(new Cart(1L, CUSTOMER, null).isEmpty()).isTrue();
        }

        @Test
        void cartRequiresCustomer() {
            assertThatThrownBy(() -> Cart.empty(1L, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void cartItemValidatesQuantityAndProduct() {
            assertThatThrownBy(() -> new CartItem(TUNA, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CartItem(null, 1)).isInstanceOf(NullPointerException.class);
            assertThat(new CartItem(TUNA, 3).subtotal()).isEqualByComparingTo("0.30");
        }

        @Test
        void checkoutCreatesNewOrderWithSnapshotOfPrices() {
            Order order = Cart.empty(1L, CUSTOMER).addProduct(CATNIP, 2).checkout(10L, NOW);

            assertThat(order.status()).isEqualTo(OrderStatus.NEW);
            assertThat(order.createdAt()).isEqualTo(NOW);
            assertThat(order.items()).containsExactly(new OrderItem(2L, "Nebula Catnip", new BigDecimal("0.20"), 2));
            assertThat(order.total()).isEqualByComparingTo("0.40");
        }

        @Test
        void emptyCartCannotBeCheckedOut() {
            assertThatThrownBy(() -> Cart.empty(1L, CUSTOMER).checkout(10L, NOW))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class OrderBehaviour {

        private Order newOrder() {
            return Order.place(10L, CUSTOMER, List.of(new OrderItem(1L, "Star Tuna", new BigDecimal("2.50"), 2)), NOW);
        }

        @Test
        void followsHappyPathLifecycle() {
            assertThat(newOrder().pay().ship().deliver().status()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        void canBeCancelledBeforeShipping() {
            assertThat(newOrder().cancel().status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(newOrder().pay().cancel().status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void illegalTransitionsAreRejected() {
            assertThatThrownBy(() -> newOrder().ship()).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot change order status from NEW to SHIPPED");
            assertThatThrownBy(() -> newOrder().pay().ship().cancel()).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> newOrder().cancel().pay()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void totalIsDerivedFromItems() {
            assertThat(newOrder().total()).isEqualByComparingTo("5.00");
        }

        @Test
        void itemsAreCopiedDefensively() {
            List<OrderItem> items = new ArrayList<>(List.of(new OrderItem(1L, "Star", BigDecimal.ONE, 1)));
            Order order = Order.place(1L, CUSTOMER, items, NOW);
            items.clear();

            assertThat(order.items()).hasSize(1);
        }

        @Test
        void orderRequiresItemsAndMandatoryFields() {
            assertThatThrownBy(() -> Order.place(1L, CUSTOMER, List.of(), NOW))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Order.place(1L, CUSTOMER, null, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
            List<OrderItem> items = List.of(new OrderItem(1L, "Star", BigDecimal.ONE, 1));
            assertThatThrownBy(() -> Order.place(1L, null, items, NOW)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Order.place(1L, CUSTOMER, items, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Order(1L, CUSTOMER, items, null, NOW)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void orderItemValidatesInput() {
            assertThatThrownBy(() -> new OrderItem(1L, "Star", BigDecimal.ONE, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new OrderItem(null, "Star", BigDecimal.ONE, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new OrderItem(1L, null, BigDecimal.ONE, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new OrderItem(1L, "Star", null, 1))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0} -> {1}: {2}")
        @CsvSource({
                "NEW, PAID, true", "NEW, CANCELLED, true", "NEW, SHIPPED, false",
                "PAID, SHIPPED, true", "PAID, CANCELLED, true", "PAID, NEW, false",
                "SHIPPED, DELIVERED, true", "SHIPPED, CANCELLED, false",
                "DELIVERED, NEW, false", "CANCELLED, PAID, false"})
        void statusTransitionRules(OrderStatus from, OrderStatus to, boolean allowed) {
            assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        }
    }
}
