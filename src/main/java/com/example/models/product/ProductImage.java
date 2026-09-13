package com.example.models.product;

/** The picture of a ticket type as uploaded, e.g. with contentType "image/png". */
public record ProductImage(
        String contentType,
        byte[] data
) { }
