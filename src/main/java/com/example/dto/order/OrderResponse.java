package com.example.dto.order;

import com.example.models.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// INFO - outbound only.
public record OrderResponse(
        int orderId,
        int userId,
        List<OrderItemResponse> items,
        BigDecimal total,
        LocalDateTime orderDate,
        LocalDateTime paymentDueDate,
        LocalDateTime paymentDate,
        OrderStatus status
) { }
