package com.cosmocats.marketplace.persistence.adapter;

import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.persistence.entity.CustomerEntity;
import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.persistence.entity.OrderItemEntity;
import com.cosmocats.marketplace.persistence.repository.CustomerJpaRepository;
import com.cosmocats.marketplace.persistence.repository.OrderJpaRepository;
import com.cosmocats.marketplace.persistence.repository.ProductJpaRepository;
import com.cosmocats.marketplace.repository.OrderRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orders;
    private final CustomerJpaRepository customers;
    private final ProductJpaRepository products;

    public JpaOrderRepositoryAdapter(OrderJpaRepository orders, CustomerJpaRepository customers,
                                     ProductJpaRepository products) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
    }

    @Override
    public Order save(Order order) {
        if (order.id() != null) {
            throw new IllegalArgumentException("A new order must not have an id");
        }
        CustomerEntity customer = customers.findByEmail(order.customerEmail())
                .orElseGet(() -> customers.save(new CustomerEntity(order.customerEmail())));
        OrderEntity entity = new OrderEntity(order.orderNumber(), customer, order.status(), order.createdAt());
        for (OrderItem item : order.items()) {
            entity.addItem(new OrderItemEntity(products.getReferenceById(item.productId()),
                    item.unitPrice(), item.quantity()));
        }
        return EntityMapper.toDomain(orders.saveAndFlush(entity));
    }

    @Override
    public Optional<Order> findByOrderNumber(UUID orderNumber) {
        return orders.findByNaturalId(orderNumber).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Order> updateStatus(UUID orderNumber, OrderStatus status) {
        return orders.findByNaturalId(orderNumber).map(entity -> {
            entity.setStatus(status);
            orders.flush();
            return EntityMapper.toDomain(entity);
        });
    }

    @Override
    public boolean deleteByOrderNumber(UUID orderNumber) {
        return orders.findByNaturalId(orderNumber).map(entity -> {
            orders.delete(entity);
            orders.flush();
            return true;
        }).orElse(false);
    }

    @Override
    public List<ProductSalesDto> findTopSellingProducts(Collection<OrderStatus> statuses, int limit) {
        return orders.findTopSellingProducts(statuses, Limit.of(limit)).stream()
                .map(view -> new ProductSalesDto(view.getProductId(), view.getProductName(),
                        view.getTotalQuantity(), view.getTotalRevenue()))
                .toList();
    }
}
