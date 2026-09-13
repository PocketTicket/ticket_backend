package com.example.dto.ticket;

import com.example.models.ticket.TicketCheckResult;

import java.time.LocalDateTime;

public record TicketCheckResponse(
        TicketCheckResult result,
        // All fields below are null if the code belongs to no ticket.
        String productName,
        String location,
        LocalDateTime startsAt,
        // From when the ticket is accepted at the entrance
        LocalDateTime entryFrom,
        // When the ticket was used; null while unused
        LocalDateTime usedAt
) { }
