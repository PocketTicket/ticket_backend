package com.example.models.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Product(
        int productId,
        String name,
        String description,
        BigDecimal price,
        String location,
        LocalDateTime startsAt,
        int maxTickets,
        // Tickets held by pending and paid orders.
        int allocatedTickets
) {
    /** Never negative, even if an admin lowered maxTickets below what is already allocated. */
    public int availableTickets() {
        return Math.max(0, maxTickets - allocatedTickets);
    }
}
