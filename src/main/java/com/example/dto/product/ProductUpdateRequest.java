package com.example.dto.product;

public record ProductUpdateRequest(
        String productName,
        String productDescription,
        double productPrice,
        int productStock
) { }