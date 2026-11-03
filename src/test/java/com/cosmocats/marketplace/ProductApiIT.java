package com.cosmocats.marketplace;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end product flow through controller, service, JPA adapters and PostgreSQL. */
class ProductApiIT extends AbstractIt {

    private static final String PRODUCTS = "/api/v1/products";

    private static String galaxyMilk(long categoryId) {
        return """
                {"name": "Galaxy Milk", "price": 3.50, "categoryId": %d, "sellerEmail": "milk@cosmocats.space"}
                """.formatted(categoryId);
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        long toys = createCategory("Zero-G Toys");
        createProduct("Star Tuna Can", "9.99", toys);

        String created = postJson(PRODUCTS, galaxyMilk(toys));
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Zero-G Toys"))
                .andExpect(jsonPath("$.price").value(3.50));

        mockMvc.perform(get(PRODUCTS).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Galaxy Milk"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(galaxyMilk(toys).replace("Galaxy Milk", "Galaxy Milk XL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Galaxy Milk XL"));

        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void createdProductHasLocationHeader() throws Exception {
        long toys = createCategory("Zero-G Toys");

        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(galaxyMilk(toys)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(PRODUCTS + "/")));
    }

    @Test
    void sameNameInSameCategoryIsConflict() throws Exception {
        long toys = createCategory("Zero-G Toys");
        postJson(PRODUCTS, galaxyMilk(toys));

        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(galaxyMilk(toys)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/product-already-exists"));
    }

    @Test
    void sameNameInAnotherCategoryIsAllowed() throws Exception {
        long toys = createCategory("Zero-G Toys");
        long food = createCategory("Space Food");
        postJson(PRODUCTS, galaxyMilk(toys));

        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(galaxyMilk(food)))
                .andExpect(status().isCreated());
    }

    @Test
    void unknownCategoryIsRejectedByService() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(galaxyMilk(424242)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/category-not-found"));
    }

    @Test
    void priceIsConvertedWithRateFromStubbedService() throws Exception {
        long id = createProduct("Star Tuna Can", "9.99", createCategory("Space Food"));
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/UAH"))
                .willReturn(okJson("{\"currency\": \"UAH\", \"rate\": 41.50}")));

        mockMvc.perform(get(PRODUCTS + "/" + id + "/price").param("currency", "UAH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(414.58))
                .andExpect(jsonPath("$.rate").value(41.50));

        exchangeService.verify(getRequestedFor(urlEqualTo("/api/v1/rates/UAH")));
    }

    @Test
    void currencyUnknownToExchangeServiceIsUnprocessable() throws Exception {
        long id = createProduct("Star Tuna Can", "9.99", createCategory("Space Food"));
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/XYZ"))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get(PRODUCTS + "/" + id + "/price").param("currency", "XYZ"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unsupported-currency"));
    }

    @Test
    void exchangeServiceOutageIsBadGateway() throws Exception {
        long id = createProduct("Star Tuna Can", "9.99", createCategory("Space Food"));
        exchangeService.stubFor(WireMock.get(urlEqualTo("/api/v1/rates/EUR"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get(PRODUCTS + "/" + id + "/price").param("currency", "EUR"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/external-service"));
    }
}
