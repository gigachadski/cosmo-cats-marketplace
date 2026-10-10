package com.cosmocats.marketplace.dto;

import java.math.BigDecimal;

public record ProductResponseDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        CategoryDto category,
        String sellerEmail) {
}
