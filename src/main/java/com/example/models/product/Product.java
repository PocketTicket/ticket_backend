package com.example.models.product;

import java.math.BigDecimal;

// TODO ADD A PRODUCT IMAGE MAYBE??
public record Product(
        int productId,
        String name,
        String description,
        BigDecimal price,
        int stock
) { }
