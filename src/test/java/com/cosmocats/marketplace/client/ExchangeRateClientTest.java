package com.cosmocats.marketplace.client;

import com.cosmocats.marketplace.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RestClient talking to the third-party exchange service, stubbed with WireMock. */
class ExchangeRateClientTest {

    @RegisterExtension
    static WireMockExtension exchangeService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ExchangeRateClient client;

    @BeforeEach
    void setUp() {
        ExchangeRateClientProperties properties = new ExchangeRateClientProperties(
                exchangeService.baseUrl(), Duration.ofSeconds(1), Duration.ofMillis(500));
        RestClient restClient = new RestClientConfig().exchangeRateRestClient(RestClient.builder(), properties);
        client = new ExchangeRateClient(restClient);
    }

    @Test
    void returnsRateFromService() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR"))
                .willReturn(okJson("{\"currency\": \"EUR\", \"rate\": 0.92}")));

        assertThat(client.getRate("EUR")).isEqualByComparingTo(new BigDecimal("0.92"));

        exchangeService.verify(getRequestedFor(urlEqualTo("/api/v1/rates/EUR"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void serverErrorBecomesExternalServiceException() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.getRate("EUR"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Exchange rate service responded with status 503");
    }

    @Test
    void unknownCurrencyBecomesExternalServiceException() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/XYZ")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getRate("XYZ"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("404");
    }

    @Test
    void responseWithoutRateIsRejected() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR")).willReturn(okJson("{\"currency\": \"EUR\"}")));

        assertThatThrownBy(() -> client.getRate("EUR"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Exchange rate service returned no rate for EUR");
    }

    @Test
    void emptyBodyIsRejected() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR")).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> client.getRate("EUR")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void brokenConnectionBecomesExternalServiceException() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.getRate("EUR"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Exchange rate service is unavailable");
    }

    @Test
    void slowResponseTimesOut() {
        exchangeService.stubFor(get(urlEqualTo("/api/v1/rates/EUR"))
                .willReturn(okJson("{\"currency\": \"EUR\", \"rate\": 0.92}").withFixedDelay(2_000)));

        assertThatThrownBy(() -> client.getRate("EUR"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Exchange rate service is unavailable");
    }
}
