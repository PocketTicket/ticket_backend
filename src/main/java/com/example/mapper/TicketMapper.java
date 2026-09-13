package com.example.mapper;

import com.example.dto.ticket.TicketResponse;
import com.example.models.ticket.Ticket;

import java.util.List;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.ticketId(),
                ticket.code(),
                ticket.orderId(),
                ticket.productId(),
                ticket.productName(),
                ticket.status(),
                ticket.issuedAt(),
                ticket.usedAt()
        );
    }

    public static List<TicketResponse> toResponses(List<Ticket> tickets) {
        return tickets.stream().map(TicketMapper::toResponse).toList();
    }
}
