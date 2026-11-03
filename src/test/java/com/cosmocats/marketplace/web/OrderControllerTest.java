package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.dto.OrderItemDto;
import com.cosmocats.marketplace.dto.OrderResponseDto;
import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.exception.InvalidOrderStatusTransitionException;
import com.cosmocats.marketplace.exception.OrderNotFoundException;
import com.cosmocats.marketplace.exception.UnknownProductException;
import com.cosmocats.marketplace.service.OrderAction;
import com.cosmocats.marketplace.service.OrderService;
import com.cosmocats.marketplace.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderController.class, ReportController.class})
class OrderControllerTest {

    private static final String ORDERS = "/api/v1/orders";
    private static final String ERRORS = "https://cosmo-cats.market/errors/";
    private static final UUID NUMBER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String VALID_ORDER = """
            {"customerEmail": "cat@cosmocats.space", "items": [{"productId": 1, "quantity": 2}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ReportService reportService;

    private static OrderResponseDto order(OrderStatus status) {
        return new OrderResponseDto(NUMBER, "cat@cosmocats.space", status, Instant.parse("2026-11-01T10:00:00Z"),
                List.of(new OrderItemDto(1L, "Star Tuna", new BigDecimal("2.50"), 2, new BigDecimal("5.00"))),
                new BigDecimal("5.00"));
    }

    @Test
    void validOrderIsPlacedAndAddressedByOrderNumber() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(order(OrderStatus.NEW));

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(ORDERS + "/" + NUMBER)))
                .andExpect(jsonPath("$.orderNumber").value(NUMBER.toString()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.total").value(5.00));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "missing email|{\"items\": [{\"productId\": 1, \"quantity\": 1}]}|customerEmail",
            "invalid email|{\"customerEmail\": \"cat\", \"items\": [{\"productId\": 1, \"quantity\": 1}]}|customerEmail",
            "no items|{\"customerEmail\": \"cat@cosmocats.space\", \"items\": []}|items",
            "missing product|{\"customerEmail\": \"cat@cosmocats.space\", \"items\": [{\"quantity\": 1}]}|items[0].productId",
            "zero quantity|{\"customerEmail\": \"cat@cosmocats.space\", \"items\": [{\"productId\": 1, \"quantity\": 0}]}|items[0].quantity",
            "huge quantity|{\"customerEmail\": \"cat@cosmocats.space\", \"items\": [{\"productId\": 1, \"quantity\": 1001}]}|items[0].quantity"})
    void invalidOrderIsRejected(String scenario, String json, String field) throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORS + "validation"))
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verifyNoInteractions(orderService);
    }

    @Test
    void unknownProductIsUnprocessable() throws Exception {
        when(orderService.placeOrder(any())).thenThrow(new UnknownProductException(1L));

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value(ERRORS + "unknown-product"));
    }

    @Test
    void orderIsReturnedByNumber() throws Exception {
        when(orderService.getOrder(NUMBER)).thenReturn(order(OrderStatus.PAID));

        mockMvc.perform(get(ORDERS + "/" + NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Star Tuna"));
    }

    @Test
    void missingOrderIsNotFound() throws Exception {
        when(orderService.getOrder(NUMBER)).thenThrow(new OrderNotFoundException(NUMBER));

        mockMvc.perform(get(ORDERS + "/" + NUMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERRORS + "order-not-found"));
    }

    @Test
    void malformedOrderNumberIsBadRequest() throws Exception {
        mockMvc.perform(get(ORDERS + "/42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORS + "bad-request"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"pay, PAY, PAID", "ship, SHIP, SHIPPED", "deliver, DELIVER, DELIVERED", "cancel, CANCEL, CANCELLED"})
    void statusActionsAreDelegated(String path, OrderAction action, OrderStatus result) throws Exception {
        when(orderService.changeStatus(NUMBER, action)).thenReturn(order(result));

        mockMvc.perform(post(ORDERS + "/" + NUMBER + "/" + path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(result.name()));
    }

    @Test
    void unknownActionIsRejected() throws Exception {
        mockMvc.perform(post(ORDERS + "/" + NUMBER + "/refund"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("action"));
        verifyNoInteractions(orderService);
    }

    @Test
    void illegalTransitionIsConflict() throws Exception {
        when(orderService.changeStatus(NUMBER, OrderAction.PAY))
                .thenThrow(new InvalidOrderStatusTransitionException(OrderStatus.PAID, OrderStatus.PAID));

        mockMvc.perform(post(ORDERS + "/" + NUMBER + "/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS + "invalid-order-status"))
                .andExpect(jsonPath("$.detail").value("Cannot change order status from PAID to PAID"));
    }

    @Test
    void deleteReturnsNoContentOrNotFound() throws Exception {
        mockMvc.perform(delete(ORDERS + "/" + NUMBER)).andExpect(status().isNoContent());

        UUID missing = UUID.randomUUID();
        doThrow(new OrderNotFoundException(missing)).when(orderService).deleteOrder(missing);
        mockMvc.perform(delete(ORDERS + "/" + missing)).andExpect(status().isNotFound());
    }

    @Test
    void topProductsReportUsesDefaultLimit() throws Exception {
        when(reportService.getTopSellingProducts(10))
                .thenReturn(List.of(new ProductSalesDto(1L, "Star Tuna", 7, new BigDecimal("17.50"))));

        mockMvc.perform(get("/api/v1/reports/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Star Tuna"))
                .andExpect(jsonPath("$[0].totalQuantity").value(7));
    }

    @ParameterizedTest(name = "limit={0}")
    @ValueSource(strings = {"0", "101"})
    void reportLimitIsValidated(String limit) throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products").param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
        verifyNoInteractions(reportService);
    }
}
