package com.example.dto.order;

import java.util.List;

// INFO - just an object
public record OrderRequest(
        int userId,
        List<OrderItemRequest> items,
        double total
) { }
