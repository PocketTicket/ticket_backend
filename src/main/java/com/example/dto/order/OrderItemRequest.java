package com.example.dto.order;

import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @Positive(message = "productId must be a positive number")
        int productId,

        @Positive(message = "quantity must be at least 1")
        int quantity
) { }
