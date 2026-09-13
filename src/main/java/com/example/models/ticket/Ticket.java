package com.example.models.ticket;

import java.math.BigDecimal;

/** Admits one person. {@code code} is the content of the QR code. */
public record Ticket(
        int ticketId,
        String code,
        int productId,
        String productName,
        // The price at order time, not the current one of the product.
        BigDecimal price
) { }
