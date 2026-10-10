package com.cosmocats.marketplace.client;

import com.cosmocats.marketplace.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/** Client of the third-party Galactic Exchange service that provides currency rates. */
@Component
public class ExchangeRateClient {

    private final RestClient restClient;

    public ExchangeRateClient(@Qualifier("exchangeRateRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public BigDecimal getRate(String currency) {
        try {
            ExchangeRateResponse response = restClient.get()
                    .uri("/api/v1/rates/{currency}", currency)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new ExternalServiceException(
                                "Exchange rate service responded with status " + clientResponse.getStatusCode().value());
                    })
                    .body(ExchangeRateResponse.class);

            if (response == null || response.rate() == null) {
                throw new ExternalServiceException("Exchange rate service returned no rate for " + currency);
            }
            return response.rate();
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Exchange rate service is unavailable", e);
        }
    }

    public record ExchangeRateResponse(String currency, BigDecimal rate) {
    }
}
