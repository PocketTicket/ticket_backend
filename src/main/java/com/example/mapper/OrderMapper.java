package com.example.mapper;

import com.example.dto.order.OrderItemResponse;
import com.example.dto.order.OrderResponse;
import com.example.models.order.Order;
import com.example.models.order.OrderItem;

import java.util.List;

/**
 * The only place that knows both the Order model and its DTOs.
 * There is no toModel(OrderRequest) on purpose: building an Order needs the
 * product prices, which is business logic and therefore lives in OrderService.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.orderId(),
                order.userId(),
                toItemResponses(order.items()),
                order.totalAmount(),
                order.orderDate(),
                order.paymentDueDate(),
                order.paymentDate(),
                order.status()
        );
    }

    public static List<OrderResponse> toResponses(List<Order> orders) {
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    private static List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemResponse(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.unitPrice(),
                        item.lineTotal()
                ))
                .toList();
    }
}
