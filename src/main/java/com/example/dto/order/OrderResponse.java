package com.example.dto.order;

import com.example.models.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

// INFO - just an object
public record OrderResponse(
        int orderId,
        int userId,
        List<OrderItemRequest> items,
        double total,
        LocalDateTime orderDate,
        LocalDateTime dueDate,
        OrderStatus status
) { }
