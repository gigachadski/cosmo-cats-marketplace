package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Reports for the Intergalactic Committee. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    /** A product counts as bought once the order is paid; new and cancelled orders do not count. */
    static final Set<OrderStatus> PURCHASED = EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<ProductSalesDto> getTopSellingProducts(int limit) {
        return orderRepository.findTopSellingProducts(PURCHASED, limit);
    }
}
