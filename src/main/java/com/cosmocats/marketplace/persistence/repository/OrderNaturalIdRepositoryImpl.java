package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;

import java.util.Optional;
import java.util.UUID;

class OrderNaturalIdRepositoryImpl implements OrderNaturalIdRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<OrderEntity> findByNaturalId(UUID orderNumber) {
        // Hibernate resolves @NaturalId through its natural id API (and natural id cache, if enabled)
        return entityManager.unwrap(Session.class)
                .bySimpleNaturalId(OrderEntity.class)
                .loadOptional(orderNumber);
    }
}
