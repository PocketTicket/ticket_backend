package com.example.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must not be negative")
        BigDecimal price,

        @PositiveOrZero(message = "stock must not be negative")
        int stock
) { }
