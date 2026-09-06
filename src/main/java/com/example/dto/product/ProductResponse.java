package com.example.dto.product;

import java.math.BigDecimal;

// INFO - outbound only.
public record ProductResponse(
        int productId,
        String name,
        String description,
        BigDecimal price,
        int stock
) { }
