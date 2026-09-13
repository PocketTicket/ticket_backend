package com.example.models.ticket;

import java.time.LocalDateTime;

/**
 * One admission. {@code code} is the payload of the QR code the guest shows at
 * the door; {@code usedAt} is set exactly when the status becomes USED.
 */
public record Ticket(
        int ticketId,
        String code,
        int orderId,
        int productId,
        String productName,
        TicketStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt
) { }
