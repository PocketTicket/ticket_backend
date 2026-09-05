package com.example.dto.product;

public record ProductCreateRequest(
        String productName,
        String productDescription,
        double productPrice,
        int productStock
) { }
