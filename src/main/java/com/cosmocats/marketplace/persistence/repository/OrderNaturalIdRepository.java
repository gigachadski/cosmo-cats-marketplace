package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.OrderEntity;

import java.util.Optional;
import java.util.UUID;

/** Custom Spring Data fragment: loads an order by its natural id instead of the surrogate key. */
public interface OrderNaturalIdRepository {

    Optional<OrderEntity> findByNaturalId(UUID orderNumber);
}
