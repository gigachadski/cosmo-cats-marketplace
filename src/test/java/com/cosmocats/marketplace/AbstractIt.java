package com.cosmocats.marketplace;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for thin integration tests: the whole application against PostgreSQL in
 * Testcontainers, with the third-party exchange rate service replaced by a WireMock stub.
 * The context (and the container) is shared by all subclasses; tables are emptied before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIt {

    @RegisterExtension
    protected static WireMockExtension exchangeService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void exchangeServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("cosmo.clients.exchange-rate.base-url", exchangeService::baseUrl);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE order_item, orders, customer, product, category");
    }

    protected long createCategory(String name) throws Exception {
        return postAndReadId("/api/v1/categories", "{\"name\": \"" + name + "\"}", "$.id");
    }

    protected long createProduct(String name, String price, long categoryId) throws Exception {
        String body = """
                {"name": "%s", "price": %s, "categoryId": %d, "sellerEmail": "seller@cosmocats.space"}
                """.formatted(name, price, categoryId);
        return postAndReadId("/api/v1/products", body, "$.id");
    }

    protected String postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long postAndReadId(String url, String body, String idPath) throws Exception {
        return ((Number) JsonPath.read(postJson(url, body), idPath)).longValue();
    }
}
