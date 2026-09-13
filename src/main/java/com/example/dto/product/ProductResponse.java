package com.example.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        int productId,
        String name,
        String description,
        BigDecimal price,
        String location,
        LocalDateTime startsAt,
        int maxTickets,
        int availableTickets
) { }
