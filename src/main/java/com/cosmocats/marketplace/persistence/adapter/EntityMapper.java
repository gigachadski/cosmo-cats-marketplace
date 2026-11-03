package com.cosmocats.marketplace.persistence.adapter;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.persistence.entity.OrderItemEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;

/** Entity to domain conversion; must be called inside a transaction (lazy associations). */
final class EntityMapper {

    private EntityMapper() {
    }

    static Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName());
    }

    static Product toDomain(ProductEntity entity) {
        return new Product(entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                toDomain(entity.getCategory()), entity.getSellerEmail());
    }

    static Order toDomain(OrderEntity entity) {
        return new Order(entity.getId(), entity.getOrderNumber(), entity.getCustomer().getEmail(),
                entity.getItems().stream().map(EntityMapper::toDomain).toList(),
                entity.getStatus(), entity.getCreatedAt());
    }

    private static OrderItem toDomain(OrderItemEntity entity) {
        return new OrderItem(entity.getProduct().getId(), entity.getProduct().getName(),
                entity.getUnitPrice(), entity.getQuantity());
    }
}
