package com.example.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** No user id: the order always belongs to the logged-in user. */
public record OrderRequest(
        @NotEmpty(message = "an order needs at least one item")
        List<@Valid OrderItemRequest> items
) { }
