package com.example.dto.order;

import com.example.models.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Carries no ticket codes: those only reach the customer by email once the order is paid. */
public record OrderResponse(
        int orderId,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal total,
        LocalDateTime createdAt,
        LocalDateTime paymentDueAt,
        LocalDateTime paidAt
) { }
