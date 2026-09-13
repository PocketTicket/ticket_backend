package com.example.ticket;

import com.example.models.order.OrderStatus;
import com.example.models.product.Product;
import com.example.models.ticket.Ticket;
import com.example.models.ticket.TicketStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Decides whether a ticket may be admitted right now.
 *
 * <p>Deliberately free of database access: it is handed everything it needs and
 * returns a verdict, which keeps every rule in one readable place and makes the
 * awkward cases (late entry shown too early, order cancelled after the ticket was
 * issued) testable without a running Postgres.
 *
 * <p>Consuming the ticket is not done here. A validator that also mutated would
 * be unable to protect against two doors scanning the same code at the same
 * moment - that race is settled by the conditional UPDATE in TicketService.
 */
@ApplicationScoped
public class TicketValidator {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * @param ticket      the ticket behind the scanned code
     * @param product     the ticket type, which carries the entry window
     * @param orderStatus status of the order the ticket was issued for
     * @param now         the moment of the scan
     */
    public TicketValidationResult validate(Ticket ticket,
                                           Product product,
                                           OrderStatus orderStatus,
                                           LocalDateTime now) {

        // Checked before anything else: this is the case the door staff sees most
        // often and the one where the message has to name the exact time, because
        // it usually means someone passed their code on.
        if (ticket.status() == TicketStatus.USED) {
            return TicketValidationResult.rejected(
                    TicketRejectionReason.ALREADY_USED,
                    "Ticket was already used on " + TIME.format(ticket.usedAt()));
        }

        if (ticket.status() == TicketStatus.CANCELLED) {
            return TicketValidationResult.rejected(
                    TicketRejectionReason.TICKET_CANCELLED,
                    "Ticket was cancelled and is not valid");
        }

        // Tickets are only issued once an order is paid, but the order can still be
        // cancelled or revoked afterwards, so the current status decides.
        if (orderStatus != OrderStatus.PAID && orderStatus != OrderStatus.DELIVERED) {
            return TicketValidationResult.rejected(
                    TicketRejectionReason.ORDER_NOT_PAID,
                    "The order for this ticket is " + orderStatus + ", not paid");
        }

        if (product.validFrom() != null && now.isBefore(product.validFrom())) {
            return TicketValidationResult.rejected(
                    TicketRejectionReason.NOT_YET_VALID,
                    "\"" + product.name() + "\" admits from "
                            + TIME.format(product.validFrom()) + " onwards");
        }

        if (product.validUntil() != null && now.isAfter(product.validUntil())) {
            return TicketValidationResult.rejected(
                    TicketRejectionReason.EXPIRED,
                    "\"" + product.name() + "\" was only valid until "
                            + TIME.format(product.validUntil()));
        }

        return TicketValidationResult.accepted();
    }
}
