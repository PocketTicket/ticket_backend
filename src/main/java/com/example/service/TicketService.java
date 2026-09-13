package com.example.service;

import com.example.dto.ticket.TicketCheckResponse;
import com.example.mapper.TicketMapper;
import com.example.models.ticket.TicketCheckResult;
import com.example.models.ticket.TicketEntry;
import com.example.repository.SettingsRepository;
import com.example.repository.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
@Transactional
public class TicketService {
    @Inject
    TicketRepository ticketRepository;

    @Inject
    SettingsRepository settingsRepository;

    /** What anyone who opens the link of a QR code sees. Never uses the ticket up. */
    public TicketCheckResponse checkTicket(String code) {
        TicketEntry ticket = ticketRepository.getTicketEntry(code);
        return toResponse(ticket, check(ticket, LocalDateTime.now()));
    }

    /** The check at the entrance: uses the ticket up if it lets the guest in right now. */
    public TicketCheckResponse checkIn(String code) {
        LocalDateTime now = LocalDateTime.now();
        TicketEntry ticket = ticketRepository.getTicketEntry(code);
        TicketCheckResult result = check(ticket, now);

        if (result == TicketCheckResult.VALID) {
            // Fails if another scanner used the ticket a moment ago.
            result = ticketRepository.markTicketAsUsed(code, now)
                    ? TicketCheckResult.ADMITTED
                    : TicketCheckResult.ALREADY_USED;
            ticket = ticketRepository.getTicketEntry(code);
        }
        return toResponse(ticket, result);
    }

    /** The rules of the entrance: a paid ticket lets one guest in, from the entry time of its event on. */
    private TicketCheckResult check(TicketEntry ticket, LocalDateTime now) {
        if (ticket == null || !ticket.paid()) {
            return TicketCheckResult.INVALID;
        }
        if (ticket.usedAt() != null) {
            return TicketCheckResult.ALREADY_USED;
        }
        if (now.isBefore(entryFrom(ticket))) {
            return TicketCheckResult.NOT_YET_OPEN;
        }
        return TicketCheckResult.VALID;
    }

    private LocalDateTime entryFrom(TicketEntry ticket) {
        return ticket.startsAt().minusMinutes(settingsRepository.getSettings().entryMinutesBeforeStart());
    }

    private TicketCheckResponse toResponse(TicketEntry ticket, TicketCheckResult result) {
        return TicketMapper.toCheckResponse(ticket, result, ticket == null ? null : entryFrom(ticket));
    }
}
