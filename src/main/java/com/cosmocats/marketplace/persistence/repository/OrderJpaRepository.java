package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>, OrderNaturalIdRepository {

    /**
     * Report for the Intergalactic Committee: which products are bought most often.
     * JPQL with WHERE (only orders in the given statuses), GROUP BY and ORDER BY,
     * returning a projection instead of whole entities.
     */
    @Query("""
            select p.id as productId,
                   p.name as productName,
                   sum(i.quantity) as totalQuantity,
                   sum(i.unitPrice * i.quantity) as totalRevenue
            from OrderItemEntity i
                join i.product p
                join i.order o
            where o.status in :statuses
            group by p.id, p.name
            order by sum(i.quantity) desc, p.name asc
            """)
    List<ProductSalesView> findTopSellingProducts(@Param("statuses") Collection<OrderStatus> statuses, Limit limit);
}
