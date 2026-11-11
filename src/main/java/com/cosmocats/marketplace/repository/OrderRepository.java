package com.cosmocats.marketplace.repository;

import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.dto.ProductSalesDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Domain port for orders. Orders are addressed by their natural id, the order number. */
public interface OrderRepository {

    /** Saves a new order; the customer is looked up by email and created on first purchase. */
    Order save(Order order);

    Optional<Order> findByOrderNumber(UUID orderNumber);

    /** Persists the new status of an existing order; empty if it does not exist. */
    Optional<Order> updateStatus(UUID orderNumber, OrderStatus status);

    /** Deletes the order with its items and reports whether it existed. */
    boolean deleteByOrderNumber(UUID orderNumber);

    /** Products ordered by total quantity sold in orders with the given statuses. */
    List<ProductSalesDto> findTopSellingProducts(Collection<OrderStatus> statuses, int limit);
}
