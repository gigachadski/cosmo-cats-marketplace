package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.OrderItemDto;
import com.cosmocats.marketplace.dto.OrderItemRequestDto;
import com.cosmocats.marketplace.dto.OrderRequestDto;
import com.cosmocats.marketplace.dto.OrderResponseDto;
import com.cosmocats.marketplace.exception.OrderNotFoundException;
import com.cosmocats.marketplace.exception.UnknownProductException;
import com.cosmocats.marketplace.repository.OrderRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    /**
     * Places an order with the current product prices. Everything happens in one transaction:
     * if any product is missing, nothing is saved, not even a new customer.
     */
    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto request) {
        List<OrderItem> items = mergeQuantities(request.items()).entrySet().stream()
                .map(line -> {
                    Product product = productRepository.findById(line.getKey())
                            .orElseThrow(() -> new UnknownProductException(line.getKey()));
                    return new OrderItem(product.id(), product.name(), product.price(), line.getValue());
                })
                .toList();
        Order order = Order.place(UUID.randomUUID(), request.customerEmail().strip(), items, clock.instant());
        Order saved = orderRepository.save(order);
        log.info("Placed order {} with {} item(s)", saved.orderNumber(), saved.items().size());
        return toDto(saved);
    }

    public OrderResponseDto getOrder(UUID orderNumber) {
        return toDto(findOrder(orderNumber));
    }

    @Transactional
    public OrderResponseDto changeStatus(UUID orderNumber, OrderAction action) {
        Order changed = action.applyTo(findOrder(orderNumber));
        Order saved = orderRepository.updateStatus(orderNumber, changed.status())
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        log.info("Order {} is now {}", orderNumber, saved.status());
        return toDto(saved);
    }

    @Transactional
    public void deleteOrder(UUID orderNumber) {
        if (!orderRepository.deleteByOrderNumber(orderNumber)) {
            throw new OrderNotFoundException(orderNumber);
        }
        log.info("Deleted order {}", orderNumber);
    }

    private Order findOrder(UUID orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    /** The same product listed twice becomes one line with the summed quantity, in request order. */
    private static Map<Long, Integer> mergeQuantities(List<OrderItemRequestDto> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        items.forEach(item -> quantities.merge(item.productId(), item.quantity(), Integer::sum));
        return quantities;
    }

    private static OrderResponseDto toDto(Order order) {
        List<OrderItemDto> items = order.items().stream()
                .map(item -> new OrderItemDto(item.productId(), item.productName(), item.unitPrice(),
                        item.quantity(), item.subtotal()))
                .toList();
        return new OrderResponseDto(order.orderNumber(), order.customerEmail(), order.status(),
                order.createdAt(), items, order.total());
    }
}
