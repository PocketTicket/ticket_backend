package com.example.dto.order;

import java.math.BigDecimal;

// INFO - outbound only.
public record OrderItemResponse(
        int productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) { }
