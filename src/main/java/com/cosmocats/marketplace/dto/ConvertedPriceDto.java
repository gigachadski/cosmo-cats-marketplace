package com.cosmocats.marketplace.dto;

import java.math.BigDecimal;

public record ConvertedPriceDto(Long productId, String currency, BigDecimal rate, BigDecimal amount) {
}
