package com.cosmocats.marketplace.persistence.entity;

import com.cosmocats.marketplace.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code orderNumber} is the natural id: clients address orders by it, so the
 * surrogate {@code id} never leaves the persistence layer.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 50)
    private Long id;

    @NaturalId
    @Column(name = "order_number", nullable = false, updatable = false)
    private UUID orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {
        // for JPA
    }

    public OrderEntity(UUID orderNumber, CustomerEntity customer, OrderStatus status, Instant createdAt) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addItem(OrderItemEntity item) {
        item.attachTo(this);
        items.add(item);
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderNumber() {
        return orderNumber;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }
}
