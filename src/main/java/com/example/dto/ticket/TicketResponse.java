package com.example.dto.ticket;

import com.example.models.ticket.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        int ticketId,
        String code,
        int orderId,
        int productId,
        String productName,
        TicketStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt
) { }
