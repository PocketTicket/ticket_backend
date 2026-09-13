package com.example.mapper;

import com.example.dto.ticket.TicketCheckResponse;
import com.example.models.ticket.TicketCheckResult;
import com.example.models.ticket.TicketEntry;

import java.time.LocalDateTime;

public final class TicketMapper {

    private TicketMapper() {
    }

    /**
     * @param ticket    null if the code belongs to no ticket
     * @param entryFrom null if the code belongs to no ticket
     */
    public static TicketCheckResponse toCheckResponse(TicketEntry ticket, TicketCheckResult result,
                                                      LocalDateTime entryFrom) {
        if (ticket == null) {
            return new TicketCheckResponse(result, null, null, null, null, null);
        }
        return new TicketCheckResponse(
                result,
                ticket.productName(),
                ticket.location(),
                ticket.startsAt(),
                entryFrom,
                ticket.usedAt()
        );
    }
}
