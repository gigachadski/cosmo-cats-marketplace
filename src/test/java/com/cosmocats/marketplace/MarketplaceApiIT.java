package com.cosmocats.marketplace;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Categories, orders and the sales report, end to end against PostgreSQL. */
class MarketplaceApiIT extends AbstractIt {

    private static final String CATEGORIES = "/api/v1/categories";
    private static final String ORDERS = "/api/v1/orders";

    private String placeOrder(String email, long productId, int quantity) throws Exception {
        String body = """
                {"customerEmail": "%s", "items": [{"productId": %d, "quantity": %d}]}
                """.formatted(email, productId, quantity);
        return JsonPath.read(postJson(ORDERS, body), "$.orderNumber");
    }

    @Test
    void categoryCrudAndItsProducts() throws Exception {
        long food = createCategory("Space Food");
        createProduct("Star Tuna Can", "9.99", food);
        createProduct("Nebula Catnip", "12.00", food);

        mockMvc.perform(get(CATEGORIES + "/" + food + "/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Nebula Catnip", "Star Tuna Can")));

        mockMvc.perform(put(CATEGORIES + "/" + food).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cosmic Food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cosmic Food"));

        mockMvc.perform(get(CATEGORIES))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cosmic Food"));
    }

    @Test
    void categoryWithProductsCannotBeDeleted() throws Exception {
        long food = createCategory("Space Food");
        createProduct("Star Tuna Can", "9.99", food);

        mockMvc.perform(delete(CATEGORIES + "/" + food))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/category-in-use"));

        long empty = createCategory("Orbital Furniture");
        mockMvc.perform(delete(CATEGORIES + "/" + empty)).andExpect(status().isNoContent());
        mockMvc.perform(get(CATEGORIES + "/" + empty)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateCategoryNameIsConflict() throws Exception {
        createCategory("Space Food");

        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Space Food\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/category-already-exists"));
    }

    @Test
    void orderLifecycleByOrderNumber() throws Exception {
        long tuna = createProduct("Star Tuna Can", "9.99", createCategory("Space Food"));
        String orderNumber = placeOrder("cat@cosmocats.space", tuna, 3);

        mockMvc.perform(get(ORDERS + "/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.total").value(29.97))
                .andExpect(jsonPath("$.items[0].productName").value("Star Tuna Can"));

        mockMvc.perform(post(ORDERS + "/" + orderNumber + "/pay")).andExpect(jsonPath("$.status").value("PAID"));
        mockMvc.perform(post(ORDERS + "/" + orderNumber + "/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/invalid-order-status"));

        mockMvc.perform(delete(ORDERS + "/" + orderNumber)).andExpect(status().isNoContent());
        mockMvc.perform(get(ORDERS + "/" + orderNumber)).andExpect(status().isNotFound());
    }

    @Test
    void orderedProductCannotBeDeleted() throws Exception {
        long tuna = createProduct("Star Tuna Can", "9.99", createCategory("Space Food"));
        placeOrder("cat@cosmocats.space", tuna, 1);

        mockMvc.perform(delete("/api/v1/products/" + tuna))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/product-in-use"));
    }

    @Test
    void orderWithUnknownProductSavesNothing() throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\": \"cat@cosmocats.space\", \"items\": [{\"productId\": 999999, \"quantity\": 1}]}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unknown-product"));
    }

    @Test
    void topProductsReportCountsOnlyPaidOrders() throws Exception {
        long food = createCategory("Space Food");
        long tuna = createProduct("Star Tuna Can", "9.99", food);
        long catnip = createProduct("Nebula Catnip", "12.00", food);
        long milk = createProduct("Galaxy Milk", "3.50", food);

        mockMvc.perform(post(ORDERS + "/" + placeOrder("a@cosmocats.space", tuna, 2) + "/pay"));
        mockMvc.perform(post(ORDERS + "/" + placeOrder("b@cosmocats.space", catnip, 5) + "/pay"));
        mockMvc.perform(post(ORDERS + "/" + placeOrder("a@cosmocats.space", tuna, 1) + "/pay"));
        placeOrder("c@cosmocats.space", milk, 100); // never paid: not a purchase

        mockMvc.perform(get("/api/v1/reports/top-products").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productName").value(contains("Nebula Catnip", "Star Tuna Can")))
                .andExpect(jsonPath("$[0].totalQuantity").value(5))
                .andExpect(jsonPath("$[1].totalQuantity").value(3))
                .andExpect(jsonPath("$[1].totalRevenue").value(29.97));
    }
}
