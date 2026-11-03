package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void topProductsCountOnlyPaidShippedAndDeliveredOrders() {
        List<ProductSalesDto> rows = List.of(new ProductSalesDto(1L, "Star Tuna", 7, new BigDecimal("69.93")));
        when(orderRepository.findTopSellingProducts(
                EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED), 5)).thenReturn(rows);

        assertThat(reportService.getTopSellingProducts(5)).isEqualTo(rows);
    }
}
