package com.example.mapper;

import com.example.dto.order.OrderItemResponse;
import com.example.dto.order.OrderResponse;
import com.example.models.order.Order;
import com.example.models.order.OrderStatus;
import com.example.models.ticket.Ticket;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The only place that knows both the Order model and its DTOs.
 * There is no toModel(OrderRequest) on purpose: building an Order needs the
 * product prices and new ticket codes, which is business logic and therefore
 * lives in OrderService.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    /** For the admin panel: the status as stored. */
    public static OrderResponse toResponse(Order order) {
        return toResponse(order, order.status());
    }

    public static List<OrderResponse> toResponses(List<Order> orders) {
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    /** For the customer: an unpaid order past its due date is shown as cancelled. */
    public static OrderResponse toCustomerResponse(Order order, LocalDateTime now) {
        return toResponse(order, order.customerStatus(now));
    }

    public static List<OrderResponse> toCustomerResponses(List<Order> orders, LocalDateTime now) {
        return orders.stream().map(order -> toCustomerResponse(order, now)).toList();
    }

    private static OrderResponse toResponse(Order order, OrderStatus status) {
        return new OrderResponse(
                order.orderId(),
                order.user().firstName(),
                order.user().lastName(),
                order.user().email(),
                status,
                toItemResponses(order.tickets()),
                order.total(),
                order.createdAt(),
                order.paymentDueAt(),
                order.paidAt()
        );
    }

    /** An order stores one row per ticket, but the customer thinks in "3 x Abiball". */
    private static List<OrderItemResponse> toItemResponses(List<Ticket> tickets) {
        Map<Integer, List<Ticket>> ticketsByProduct = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::productId, LinkedHashMap::new, Collectors.toList()));

        return ticketsByProduct.values().stream()
                .map(group -> {
                    Ticket first = group.getFirst();
                    return new OrderItemResponse(first.productId(), first.productName(), group.size(), first.price());
                })
                .toList();
    }
}
