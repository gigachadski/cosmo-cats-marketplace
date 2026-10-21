package com.cosmocats.marketplace;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end flow through controller, service, mapper and in-memory repositories. */
class ProductApiIT extends AbstractIt {

    private static final String PRODUCTS = "/api/v1/products";
    private static final String GALAXY_MILK = """
            {"name": "Galaxy Milk", "price": 3.50, "categoryId": 2, "sellerEmail": "milk@cosmocats.space"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullCrudLifecycle() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(GALAXY_MILK))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(PRODUCTS + "/6")))
                .andExpect(jsonPath("$.category.name").value("Zero-G Toys"));

        mockMvc.perform(get(PRODUCTS).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Galaxy Milk"))
                .andExpect(jsonPath("$.totalElements").value(6));

        mockMvc.perform(put(PRODUCTS + "/6").contentType(MediaType.APPLICATION_JSON)
                        .content(GALAXY_MILK.replace("Galaxy Milk", "Galaxy Milk XL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Galaxy Milk XL"));

        mockMvc.perform(delete(PRODUCTS + "/6")).andExpect(status().isNoContent());
        mockMvc.perform(get(PRODUCTS + "/6")).andExpect(status().isNotFound());
    }

    @Test
    void unknownCategoryIsRejectedByService() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)
                        .content(GALAXY_MILK.replace("\"categoryId\": 2", "\"categoryId\": 42")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/category-not-found"));
    }

    @Test
    void priceIsConvertedWithRateFromStubbedService() throws Exception {
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/UAH"))
                .willReturn(okJson("{\"currency\": \"UAH\", \"rate\": 41.50}")));

        mockMvc.perform(get(PRODUCTS + "/1/price").param("currency", "UAH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(414.58))
                .andExpect(jsonPath("$.rate").value(41.50));

        exchangeService.verify(getRequestedFor(urlEqualTo("/api/v1/rates/UAH")));
    }

    @Test
    void currencyUnknownToExchangeServiceIsUnprocessable() throws Exception {
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/XYZ"))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get(PRODUCTS + "/1/price").param("currency", "XYZ"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unsupported-currency"));
    }

    @Test
    void exchangeServiceOutageIsBadGateway() throws Exception {
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/EUR"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get(PRODUCTS + "/1/price").param("currency", "EUR"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/external-service"));
    }
}
