package com.cosmocats.marketplace.dto;

import java.math.BigDecimal;

/** One row of the "most purchased products" report. */
public record ProductSalesDto(Long productId, String productName, long totalQuantity, BigDecimal totalRevenue) {
}
