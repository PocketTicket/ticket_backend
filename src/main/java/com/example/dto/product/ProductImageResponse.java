package com.example.dto.product;

public record ProductImageResponse(
        String contentType,
        byte[] data
) { }
