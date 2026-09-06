package com.example.dto.order;

import jakarta.validation.constraints.Positive;

// INFO - inbound only. The price is deliberately absent: it is looked up
// server-side from the product, never accepted from the client.
public record OrderItemRequest(
        @Positive(message = "productId must be a positive number")
        int productId,

        @Positive(message = "quantity must be at least 1")
        int quantity
) { }
