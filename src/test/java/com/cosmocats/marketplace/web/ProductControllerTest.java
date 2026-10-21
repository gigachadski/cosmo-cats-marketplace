package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ConvertedPriceDto;
import com.cosmocats.marketplace.dto.PageResponse;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.exception.ExternalServiceException;
import com.cosmocats.marketplace.exception.ProductNotFoundException;
import com.cosmocats.marketplace.exception.UnsupportedCurrencyException;
import com.cosmocats.marketplace.service.ProductService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only: the service is mocked, so these tests check request validation,
 * HTTP statuses and RFC 9457 problem responses.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCTS = "/api/v1/products";
    private static final String ERRORS = "https://cosmo-cats.market/errors/";

    private static final ProductResponseDto GALAXY_MILK = new ProductResponseDto(6L, "Galaxy Milk",
            "Anti-gravity milk", new BigDecimal("3.50"), new CategoryDto(1L, "Space Food"), "milk@cosmocats.space");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Builds a request body; {@code null} leaves the field out. */
    private static String body(String name, String description, String price, String categoryId, String email) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putIfPresent(fields, "name", name);
        putIfPresent(fields, "description", description);
        putIfPresent(fields, "price", price == null ? null : new BigDecimal(price));
        putIfPresent(fields, "categoryId", categoryId == null ? null : Long.valueOf(categoryId));
        putIfPresent(fields, "sellerEmail", email);
        return JSON.writeValueAsString(fields);
    }

    private static void putIfPresent(Map<String, Object> fields, String field, Object value) {
        if (value != null) {
            fields.put(field, value);
        }
    }

    private static String validBody() {
        return body("Galaxy Milk", "Anti-gravity milk", "3.50", "1", "milk@cosmocats.space");
    }

    private ResultActions postProduct(String json) throws Exception {
        return mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static ResultActions expectProblem(ResultActions actions, int status, String type, String instance)
            throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERRORS + type))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(instance));
    }

    @Nested
    class CreateValidation {

        @Test
        void validProductIsCreated() throws Exception {
            when(productService.createProduct(any())).thenReturn(GALAXY_MILK);

            postProduct(validBody())
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith(PRODUCTS + "/6")))
                    .andExpect(jsonPath("$.id").value(6))
                    .andExpect(jsonPath("$.name").value("Galaxy Milk"))
                    .andExpect(jsonPath("$.category.name").value("Space Food"));

            verify(productService).createProduct(new ProductRequestDto("Galaxy Milk", "Anti-gravity milk",
                    new BigDecimal("3.50"), 1L, "milk@cosmocats.space"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"Star", "Comet Treats", "SUPERNOVA STARDUST", "Moon Laser Pointer"})
        void namesWithCosmicWordsAreAccepted(String name) throws Exception {
            when(productService.createProduct(any())).thenReturn(GALAXY_MILK);

            postProduct(body(name, null, "1", "1", "a@cosmocats.space"))
                    .andExpect(status().isCreated());
        }

        @Test
        void optionalDescriptionMayBeOmitted() throws Exception {
            when(productService.createProduct(any())).thenReturn(GALAXY_MILK);

            postProduct(body("Galaxy Milk", null, "0.01", "1", "milk@cosmocats.space"))
                    .andExpect(status().isCreated());
        }

        static Stream<Arguments> invalidProducts() {
            String longDescription = "x".repeat(501);
            String longName = "Star " + "x".repeat(100);
            return Stream.of(
                    Arguments.of("missing name", body(null, null, "1", "1", "a@b.space"),
                            "name", "is required"),
                    Arguments.of("name too long", body(longName, null, "1", "1", "a@b.space"),
                            "name", "must be between 3 and 100 characters"),
                    Arguments.of("name without cosmic word", body("Tuna Can", null, "1", "1", "a@b.space"),
                            "name", "must contain a cosmic word (e.g. star, galaxy, comet)"),
                    Arguments.of("description too long", body("Star Tuna", longDescription, "1", "1", "a@b.space"),
                            "description", "must be at most 500 characters"),
                    Arguments.of("missing price", body("Star Tuna", null, null, "1", "a@b.space"),
                            "price", "is required"),
                    Arguments.of("zero price", body("Star Tuna", null, "0", "1", "a@b.space"),
                            "price", "must be greater than 0"),
                    Arguments.of("negative price", body("Star Tuna", null, "-5", "1", "a@b.space"),
                            "price", "must be greater than 0"),
                    Arguments.of("price with 3 decimals", body("Star Tuna", null, "1.999", "1", "a@b.space"),
                            "price", "must have at most 10 integer digits and 2 decimal places"),
                    Arguments.of("missing category", body("Star Tuna", null, "1", null, "a@b.space"),
                            "categoryId", "is required"),
                    Arguments.of("missing email", body("Star Tuna", null, "1", "1", null),
                            "sellerEmail", "is required"),
                    Arguments.of("invalid email", body("Star Tuna", null, "1", "1", "not-an-email"),
                            "sellerEmail", "must be a valid email address"),
                    Arguments.of("empty email", body("Star Tuna", null, "1", "1", ""),
                            "sellerEmail", "is required"),
                    Arguments.of("price with 11 integer digits", body("Star Tuna", null, "10000000000", "1", "a@b.space"),
                            "price", "must have at most 10 integer digits and 2 decimal places"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("boundaryProducts")
        void valuesExactlyOnTheLimitsAreAccepted(String scenario, String json) throws Exception {
            when(productService.createProduct(any())).thenReturn(GALAXY_MILK);

            postProduct(json).andExpect(status().isCreated());
        }

        static Stream<Arguments> boundaryProducts() {
            return Stream.of(
                    Arguments.of("name of 100 characters", body("Star " + "x".repeat(95), null, "1", "1", "a@b.space")),
                    Arguments.of("description of 500 characters", body("Star", "x".repeat(500), "1", "1", "a@b.space")),
                    Arguments.of("smallest price 0.01", body("Star", null, "0.01", "1", "a@b.space")),
                    Arguments.of("largest price 9999999999.99", body("Star", null, "9999999999.99", "1", "a@b.space")));
        }

        @Test
        void wrongJsonTypeIsReportedAsValidationErrorOfThatField() throws Exception {
            String json = "{\"name\": \"Star Tuna\", \"price\": \"abc\", \"categoryId\": 1, \"sellerEmail\": \"a@b.space\"}";

            expectProblem(postProduct(json), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.detail").value("Field 'price' must be a number."))
                    .andExpect(jsonPath("$.errors[0].field").value("price"));
            verifyNoInteractions(productService);
        }

        @Test
        void textWhereNumberIsExpectedInCategoryIdIsReported() throws Exception {
            String json = "{\"name\": \"Star Tuna\", \"price\": 1, \"categoryId\": \"food\", \"sellerEmail\": \"a@b.space\"}";

            expectProblem(postProduct(json), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be a number"));
        }

        @Test
        void objectWhereStringIsExpectedIsReported() throws Exception {
            String json = "{\"name\": {\"en\": \"Star\"}, \"price\": 1, \"categoryId\": 1, \"sellerEmail\": \"a@b.space\"}";

            expectProblem(postProduct(json), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be a string"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidProducts")
        void invalidProductIsRejectedWithValidationProblem(String scenario, String json, String field, String message)
                throws Exception {
            expectProblem(postProduct(json), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.detail").value("Field '" + field + "' " + message + "."))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value(field))
                    .andExpect(jsonPath("$.errors[0].message").value(message));

            verifyNoInteractions(productService);
        }

        @Test
        void tooShortNameBreaksBothSizeAndCosmicWordRules() throws Exception {
            // no cosmic word is shorter than 3 letters, so both constraints fire
            expectProblem(postProduct(body("St", null, "1", "1", "a@b.space")), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].message").value(contains(
                            "must be between 3 and 100 characters",
                            "must contain a cosmic word (e.g. star, galaxy, comet)")));
        }

        @Test
        void allInvalidFieldsAreReportedSortedByName() throws Exception {
            expectProblem(postProduct(body("Tuna", null, "-1", null, "bad")), 400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.errors[*].field").value(contains("categoryId", "name", "price", "sellerEmail")))
                    .andExpect(jsonPath("$.detail").value("Field 'categoryId' is required. "
                            + "Field 'name' must contain a cosmic word (e.g. star, galaxy, comet). "
                            + "Field 'price' must be greater than 0. "
                            + "Field 'sellerEmail' must be a valid email address."));
        }

        @Test
        void malformedJsonIsRejected() throws Exception {
            expectProblem(postProduct("{ \"name\": "), 400, "malformed-request", PRODUCTS)
                    .andExpect(jsonPath("$.title").value("Malformed request"));
        }

        @Test
        void missingBodyIsRejected() throws Exception {
            expectProblem(mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)),
                    400, "malformed-request", PRODUCTS);
        }

        @Test
        void unknownCategoryIsUnprocessable() throws Exception {
            when(productService.createProduct(any())).thenThrow(new CategoryNotFoundException(42L));

            expectProblem(postProduct(validBody()), 422, "category-not-found", PRODUCTS)
                    .andExpect(jsonPath("$.detail").value("Category with id 42 does not exist"));
        }

        @Test
        void unsupportedMediaTypeIsReportedAsProblem() throws Exception {
            expectProblem(mockMvc.perform(post(PRODUCTS).contentType(MediaType.TEXT_PLAIN).content("star")),
                    415, "unsupported-media-type", PRODUCTS);
        }
    }

    @Nested
    class GetProducts {

        @Test
        void defaultsArePage0Size10() throws Exception {
            when(productService.getProducts(0, 10)).thenReturn(new PageResponse<>(List.of(GALAXY_MILK), 0, 10, 1));

            mockMvc.perform(get(PRODUCTS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @ParameterizedTest(name = "page={0}, size={1}")
        @CsvSource({"0,1", "3,100"})
        void boundaryValuesAreAccepted(int page, int size) throws Exception {
            when(productService.getProducts(page, size)).thenReturn(new PageResponse<>(List.of(), page, size, 0));

            mockMvc.perform(get(PRODUCTS).param("page", String.valueOf(page)).param("size", String.valueOf(size)))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest(name = "page={0}, size={1} -> {2}")
        @CsvSource({
                "-1, 10, page, must not be negative",
                "0, 0, size, must be at least 1",
                "0, 101, size, must be at most 100"})
        void outOfRangeValuesAreRejected(String page, String size, String field, String message) throws Exception {
            expectProblem(mockMvc.perform(get(PRODUCTS).param("page", page).param("size", size)),
                    400, "validation", PRODUCTS)
                    .andExpect(jsonPath("$.errors[0].field").value(field))
                    .andExpect(jsonPath("$.errors[0].message").value(message));

            verify(productService, never()).getProducts(anyInt(), anyInt());
        }

        @Test
        void nonNumericPageIsBadRequest() throws Exception {
            expectProblem(mockMvc.perform(get(PRODUCTS).param("page", "first")), 400, "bad-request", PRODUCTS);
        }
    }

    @Nested
    class SingleProduct {

        @Test
        void existingProductIsReturned() throws Exception {
            when(productService.getProductById(6L)).thenReturn(GALAXY_MILK);

            mockMvc.perform(get(PRODUCTS + "/6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(3.50))
                    .andExpect(jsonPath("$.sellerEmail").value("milk@cosmocats.space"));
        }

        @Test
        void missingProductIsNotFound() throws Exception {
            when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

            expectProblem(mockMvc.perform(get(PRODUCTS + "/99")), 404, "product-not-found", PRODUCTS + "/99")
                    .andExpect(jsonPath("$.detail").value("Product with id 99 was not found"));
        }

        @Test
        void nonNumericIdIsBadRequest() throws Exception {
            expectProblem(mockMvc.perform(get(PRODUCTS + "/abc")), 400, "bad-request", PRODUCTS + "/abc");
        }

        @Test
        void unsupportedMethodIsReportedAsProblem() throws Exception {
            expectProblem(mockMvc.perform(patch(PRODUCTS + "/6")), 405, "method-not-allowed", PRODUCTS + "/6");
        }

        @Test
        void unexpectedErrorDoesNotLeakDetails() throws Exception {
            when(productService.getProductById(6L)).thenThrow(new IllegalStateException("db password is 123"));

            expectProblem(mockMvc.perform(get(PRODUCTS + "/6")), 500, "internal", PRODUCTS + "/6")
                    .andExpect(jsonPath("$.detail")
                            .value("An unexpected error occurred. Please contact support if the problem persists."));
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void validUpdateReturnsUpdatedProduct() throws Exception {
            when(productService.updateProduct(eq(6L), any())).thenReturn(GALAXY_MILK);

            mockMvc.perform(put(PRODUCTS + "/6").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(6));
        }

        @Test
        void invalidUpdateIsRejected() throws Exception {
            String json = body("Galaxy Milk", null, "0", "1", "milk@cosmocats.space");

            expectProblem(mockMvc.perform(put(PRODUCTS + "/6").contentType(MediaType.APPLICATION_JSON).content(json)),
                    400, "validation", PRODUCTS + "/6")
                    .andExpect(jsonPath("$.errors[0].field").value("price"));
            verifyNoInteractions(productService);
        }

        @Test
        void updateOfMissingProductIsNotFound() throws Exception {
            when(productService.updateProduct(eq(99L), any())).thenThrow(new ProductNotFoundException(99L));

            expectProblem(mockMvc.perform(put(PRODUCTS + "/99").contentType(MediaType.APPLICATION_JSON).content(validBody())),
                    404, "product-not-found", PRODUCTS + "/99");
        }

        @Test
        void deleteReturnsNoContent() throws Exception {
            mockMvc.perform(delete(PRODUCTS + "/6"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            verify(productService).deleteProduct(6L);
        }

        @Test
        void deleteOfMissingProductIsNotFound() throws Exception {
            doThrow(new ProductNotFoundException(99L)).when(productService).deleteProduct(99L);

            expectProblem(mockMvc.perform(delete(PRODUCTS + "/99")), 404, "product-not-found", PRODUCTS + "/99");
        }
    }

    @Nested
    class ConvertedPrice {

        @Test
        void validCurrencyReturnsConvertedPrice() throws Exception {
            when(productService.convertPrice(6L, "EUR"))
                    .thenReturn(new ConvertedPriceDto(6L, "EUR", new BigDecimal("2"), new BigDecimal("7.00")));

            mockMvc.perform(get(PRODUCTS + "/6/price").param("currency", "EUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(7.00))
                    .andExpect(jsonPath("$.currency").value("EUR"));
        }

        @ParameterizedTest(name = "currency={0}")
        @ValueSource(strings = {"eur", "EURO", "E1R", ""})
        void invalidCurrencyIsRejected(String currency) throws Exception {
            expectProblem(mockMvc.perform(get(PRODUCTS + "/6/price").param("currency", currency)),
                    400, "validation", PRODUCTS + "/6/price")
                    .andExpect(jsonPath("$.errors[0].field").value("currency"));
            verifyNoInteractions(productService);
        }

        @Test
        void missingCurrencyIsBadRequest() throws Exception {
            expectProblem(mockMvc.perform(get(PRODUCTS + "/6/price")), 400, "bad-request", PRODUCTS + "/6/price");
        }

        @Test
        void unknownCurrencyIsUnprocessable() throws Exception {
            when(productService.convertPrice(6L, "XYZ")).thenThrow(new UnsupportedCurrencyException("XYZ"));

            expectProblem(mockMvc.perform(get(PRODUCTS + "/6/price").param("currency", "XYZ")),
                    422, "unsupported-currency", PRODUCTS + "/6/price")
                    .andExpect(jsonPath("$.detail").value("Currency XYZ is not supported by the exchange rate service"));
        }

        @Test
        void exchangeServiceFailureIsBadGateway() throws Exception {
            when(productService.convertPrice(6L, "EUR")).thenThrow(new ExternalServiceException("down"));

            expectProblem(mockMvc.perform(get(PRODUCTS + "/6/price").param("currency", "EUR")),
                    502, "external-service", PRODUCTS + "/6/price");
        }
    }
}
