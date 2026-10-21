package com.cosmocats.marketplace.client;

import com.cosmocats.marketplace.exception.ExternalServiceException;
import com.cosmocats.marketplace.exception.UnsupportedCurrencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, clientResponse) -> {
                        throw new UnsupportedCurrencyException(currency);
                    })
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new ExternalServiceException(
                                "Exchange rate service responded with status " + clientResponse.getStatusCode().value());
                    })
                    .body(ExchangeRateResponse.class);

            if (response == null || response.rate() == null) {
                throw new ExternalServiceException("Exchange rate service returned no rate for " + currency);
            }
            if (response.rate().signum() <= 0) {
                throw new ExternalServiceException("Exchange rate service returned a non-positive rate for " + currency);
            }
            return response.rate();
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Exchange rate service is unavailable", e);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Exchange rate service returned an unreadable response", e);
        }
    }

    public record ExchangeRateResponse(String currency, BigDecimal rate) {
    }
}
