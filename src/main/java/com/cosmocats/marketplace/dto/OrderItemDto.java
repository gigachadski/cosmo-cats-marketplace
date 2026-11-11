package com.cosmocats.marketplace.dto;

import java.math.BigDecimal;

public record OrderItemDto(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
}
