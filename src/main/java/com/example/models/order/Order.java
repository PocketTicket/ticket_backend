package com.example.models.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// INFO - domain model, mirrors a row of orders plus its items.
public record Order(
        int orderId,
        int userId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        LocalDateTime orderDate,
        LocalDateTime paymentDueDate,
        LocalDateTime paymentDate,
        OrderStatus status
) { }
