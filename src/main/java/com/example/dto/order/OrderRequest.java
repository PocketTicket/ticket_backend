package com.example.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

// INFO - inbound only. The total is deliberately absent: it is computed
// server-side from the product prices, never accepted from the client.
public record OrderRequest(
        @Positive(message = "userId must be a positive number")
        int userId,

        @NotEmpty(message = "an order needs at least one item")
        List<@Valid OrderItemRequest> items
) { }
