package com.example.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderRequest(
        @Positive(message = "userId must be a positive number")
        int userId,

        @NotEmpty(message = "an order needs at least one item")
        List<@Valid OrderItemRequest> items
) { }
