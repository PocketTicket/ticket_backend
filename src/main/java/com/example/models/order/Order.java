package com.example.models.order;

import com.example.dto.order.OrderItemRequest;

import java.time.LocalDateTime;
import java.util.List;

// INFO - just an object
public record Order(
        int orderId,
        int userId,
        List<OrderItemRequest> items,
        double total,
        LocalDateTime orderDate,
        LocalDateTime dueDate,
        LocalDateTime paymentDate,
        OrderStatus status
) { }
