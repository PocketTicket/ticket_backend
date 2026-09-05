package com.example.models.product;

// TODO ADD A PRODUCT IMAGE MAYBE??
public record Product(
        int productId,
        String productName,
        String productDescription,
        double productPrice,
        int productStock
) { }
