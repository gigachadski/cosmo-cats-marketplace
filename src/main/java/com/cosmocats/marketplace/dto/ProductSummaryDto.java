package com.cosmocats.marketplace.dto;

import java.math.BigDecimal;

public record ProductSummaryDto(Long id, String name, BigDecimal price) {
}
