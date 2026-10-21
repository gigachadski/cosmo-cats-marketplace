package com.cosmocats.marketplace.client;

import com.cosmocats.marketplace.exception.UnsupportedCurrencyException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Loads the same stub mappings that docker-compose serves (wiremock/mappings),
 * so a broken stub file is caught by the build, not at demo time.
 */
class ExchangeRateStubMappingsIT {

    @RegisterExtension
    static WireMockExtension exchangeService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderDirectory("wiremock"))
            .build();

    private ExchangeRateClient client;

    @BeforeEach
    void setUp() {
        ExchangeRateClientProperties properties = new ExchangeRateClientProperties(
                exchangeService.baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(2));
        client = new ExchangeRateClient(new RestClientConfig().exchangeRateRestClient(RestClient.builder(), properties));
    }

    @Test
    void stubbedCurrenciesReturnTheirRates() {
        assertThat(client.getRate("EUR")).isEqualByComparingTo("0.92");
        assertThat(client.getRate("UAH")).isEqualByComparingTo("41.50");
    }

    @Test
    void otherCurrenciesAreUnsupported() {
        assertThatThrownBy(() -> client.getRate("JPY")).isInstanceOf(UnsupportedCurrencyException.class);
    }
}
