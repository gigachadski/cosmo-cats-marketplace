package com.cosmocats.marketplace.dto;

import com.cosmocats.marketplace.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID orderNumber,
        String customerEmail,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemDto> items,
        BigDecimal total) {
}
