package com.cosmocats.marketplace.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cosmo.clients.exchange-rate")
public record ExchangeRateClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
