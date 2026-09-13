package com.example.dto.product;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must not be negative")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price,

        @NotBlank(message = "location must not be blank")
        @Size(max = 255, message = "location must not exceed 255 characters")
        String location,

        @NotNull(message = "startsAt is required")
        LocalDateTime startsAt,

        @PositiveOrZero(message = "maxTickets must not be negative")
        int maxTickets
) { }
