package com.example.models.ticket;

import java.time.LocalDateTime;

/** A ticket as the entrance sees it: is it paid, was it used, and when and where is its event. */
public record TicketEntry(
        String code,
        // Whether the order of the ticket is paid
        boolean paid,
        // Null while the ticket has not been used
        LocalDateTime usedAt,
        String productName,
        String location,
        LocalDateTime startsAt
) { }
