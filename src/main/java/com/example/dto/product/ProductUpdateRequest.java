package com.example.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Currently identical to ProductCreateRequest, but kept separate on purpose:
// create and update diverge as soon as one of them gains a field the other
// must not accept (e.g. an event id that may be set but never moved).
public record ProductUpdateRequest(
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
