package com.example.models.order;

import com.example.models.ticket.Ticket;
import com.example.models.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Order(
        int orderId,
        User user,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime paymentDueAt,
        LocalDateTime paidAt,
        List<Ticket> tickets
) {
    public BigDecimal total() {
        return tickets.stream().map(Ticket::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * The status the customer sees. After the due date an unpaid order counts as cancelled
     * for them, even though the admin still has one more day to confirm a late payment.
     */
    public OrderStatus customerStatus(LocalDateTime now) {
        return status == OrderStatus.PENDING && now.isAfter(paymentDueAt) ? OrderStatus.CANCELLED : status;
    }
}
