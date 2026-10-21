package com.cosmocats.marketplace.dto;

import com.cosmocats.marketplace.validation.CosmicWordCheck;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating or fully replacing a product (POST / PUT).
 * Messages are predicates, so the error handler renders them as "Field 'price' must be greater than 0."
 */
public record ProductRequestDto(

        @NotNull(message = "is required")
        @Size(min = 3, max = 100, message = "must be between 3 and 100 characters")
        @CosmicWordCheck
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        String description,

        @NotNull(message = "is required")
        @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "is required")
        Long categoryId,

        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        String sellerEmail) {
}
