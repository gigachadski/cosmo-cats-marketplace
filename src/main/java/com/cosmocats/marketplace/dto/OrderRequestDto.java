package com.cosmocats.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Places a new order. The same product may appear several times; quantities are summed. */
public record OrderRequestDto(
        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        String customerEmail,

        @NotEmpty(message = "must contain at least one item")
        @Size(max = 50, message = "must contain at most 50 items")
        List<@NotNull(message = "must not be null") @Valid OrderItemRequestDto> items) {
}
