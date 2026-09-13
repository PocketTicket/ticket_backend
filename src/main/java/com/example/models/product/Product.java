package com.example.models.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TODO ADD A PRODUCT IMAGE MAYBE??
public record Product(
        int productId,
        String name,
        String description,
        BigDecimal price,
        int stock,
        // The entry window for this ticket type. Null on either side means no
        // limit in that direction, so a product with both null always admits.
        LocalDateTime validFrom,
        LocalDateTime validUntil
) { }
