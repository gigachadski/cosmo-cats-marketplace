package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.OrderRequestDto;
import com.cosmocats.marketplace.dto.OrderResponseDto;
import com.cosmocats.marketplace.service.OrderAction;
import com.cosmocats.marketplace.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Orders are addressed by their natural id (order number), never by the database id. */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto placed = orderService.placeOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderNumber}")
                .buildAndExpand(placed.orderNumber())
                .toUri();
        return ResponseEntity.created(location).body(placed);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponseDto getOrder(@PathVariable UUID orderNumber) {
        return orderService.getOrder(orderNumber);
    }

    @PostMapping("/{orderNumber}/{action}")
    public OrderResponseDto changeStatus(
            @PathVariable UUID orderNumber,
            @PathVariable
            @Pattern(regexp = "pay|ship|deliver|cancel", message = "must be one of pay, ship, deliver, cancel")
            String action) {
        return orderService.changeStatus(orderNumber, OrderAction.fromPath(action));
    }

    @DeleteMapping("/{orderNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable UUID orderNumber) {
        orderService.deleteOrder(orderNumber);
    }
}
