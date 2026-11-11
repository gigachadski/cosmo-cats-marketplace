package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.OrderItemDto;
import com.cosmocats.marketplace.dto.OrderItemRequestDto;
import com.cosmocats.marketplace.dto.OrderRequestDto;
import com.cosmocats.marketplace.dto.OrderResponseDto;
import com.cosmocats.marketplace.exception.InvalidOrderStatusTransitionException;
import com.cosmocats.marketplace.exception.OrderNotFoundException;
import com.cosmocats.marketplace.exception.UnknownProductException;
import com.cosmocats.marketplace.repository.OrderRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final UUID NUMBER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final Category FOOD = new Category(1L, "Space Food");
    private static final Product TUNA = new Product(1L, "Star Tuna", null, new BigDecimal("2.50"), FOOD, "s@cosmocats.space");
    private static final Product CATNIP = new Product(2L, "Nebula Catnip", null, new BigDecimal("4.00"), FOOD, "s@cosmocats.space");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Order savedOrder(OrderStatus status) {
        return new Order(7L, NUMBER, "cat@cosmocats.space",
                List.of(new OrderItem(1L, "Star Tuna", new BigDecimal("2.50"), 2)), status, NOW);
    }

    @Test
    void placeOrderUsesCurrentPricesAndMergesRepeatedProducts() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(TUNA));
        when(productRepository.findById(2L)).thenReturn(Optional.of(CATNIP));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderRequestDto request = new OrderRequestDto(" cat@cosmocats.space ", List.of(
                new OrderItemRequestDto(1L, 1), new OrderItemRequestDto(2L, 1), new OrderItemRequestDto(1L, 2)));

        OrderResponseDto response = orderService.placeOrder(request);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().orderNumber()).isNotNull();
        assertThat(saved.getValue().status()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
        assertThat(response.customerEmail()).isEqualTo("cat@cosmocats.space");
        assertThat(response.items()).containsExactly(
                new OrderItemDto(1L, "Star Tuna", new BigDecimal("2.50"), 3, new BigDecimal("7.50")),
                new OrderItemDto(2L, "Nebula Catnip", new BigDecimal("4.00"), 1, new BigDecimal("4.00")));
        assertThat(response.total()).isEqualByComparingTo("11.50");
    }

    @Test
    void unknownProductCancelsTheWholeOrder() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(TUNA));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        OrderRequestDto request = new OrderRequestDto("cat@cosmocats.space",
                List.of(new OrderItemRequestDto(1L, 1), new OrderItemRequestDto(99L, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(UnknownProductException.class)
                .hasMessage("Product with id 99 does not exist");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderByNaturalId() {
        when(orderRepository.findByOrderNumber(NUMBER)).thenReturn(Optional.of(savedOrder(OrderStatus.NEW)));

        OrderResponseDto response = orderService.getOrder(NUMBER);

        assertThat(response.orderNumber()).isEqualTo(NUMBER);
        assertThat(response.total()).isEqualByComparingTo("5.00");
    }

    @Test
    void missingOrderIsNotFound() {
        when(orderRepository.findByOrderNumber(NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(NUMBER))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(NUMBER.toString());
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @CsvSource({"NEW, PAY, PAID", "PAID, SHIP, SHIPPED", "SHIPPED, DELIVER, DELIVERED", "NEW, CANCEL, CANCELLED"})
    void changeStatusAppliesDomainRuleAndPersists(OrderStatus from, OrderAction action, OrderStatus to) {
        when(orderRepository.findByOrderNumber(NUMBER)).thenReturn(Optional.of(savedOrder(from)));
        when(orderRepository.updateStatus(NUMBER, to)).thenReturn(Optional.of(savedOrder(to)));

        assertThat(orderService.changeStatus(NUMBER, action).status()).isEqualTo(to);
    }

    @Test
    void illegalStatusChangeIsRejectedWithoutSaving() {
        when(orderRepository.findByOrderNumber(NUMBER)).thenReturn(Optional.of(savedOrder(OrderStatus.DELIVERED)));

        assertThatThrownBy(() -> orderService.changeStatus(NUMBER, OrderAction.CANCEL))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessage("Cannot change order status from DELIVERED to CANCELLED");
        verify(orderRepository, never()).updateStatus(any(), any());
    }

    @Test
    void orderDeletedConcurrentlyIsNotFound() {
        when(orderRepository.findByOrderNumber(NUMBER)).thenReturn(Optional.of(savedOrder(OrderStatus.NEW)));
        when(orderRepository.updateStatus(eq(NUMBER), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.changeStatus(NUMBER, OrderAction.PAY))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteRemovesOrder() {
        when(orderRepository.deleteByOrderNumber(NUMBER)).thenReturn(true);

        orderService.deleteOrder(NUMBER);

        verify(orderRepository).deleteByOrderNumber(NUMBER);
    }

    @Test
    void deleteOfMissingOrderIsNotFound() {
        when(orderRepository.deleteByOrderNumber(NUMBER)).thenReturn(false);

        assertThatThrownBy(() -> orderService.deleteOrder(NUMBER)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void actionIsParsedFromUrlSegment() {
        assertThat(OrderAction.fromPath("deliver")).isEqualTo(OrderAction.DELIVER);
        assertThatThrownBy(() -> OrderAction.fromPath("refund")).isInstanceOf(IllegalArgumentException.class);
    }
}
