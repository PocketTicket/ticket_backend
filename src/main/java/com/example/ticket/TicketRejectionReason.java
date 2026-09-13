package com.example.ticket;

/**
 * Why a ticket was turned away at the door. Kept as an enum so the scanner app
 * can react to the case (colour, sound, whether to call a supervisor) instead of
 * matching on message text.
 */
public enum TicketRejectionReason {
    /** Someone already came in with this code. */
    ALREADY_USED,
    /** The ticket was cancelled, e.g. because its order was cancelled. */
    TICKET_CANCELLED,
    /** The bank transfer for the order has not arrived yet. */
    ORDER_NOT_PAID,
    /** Correct ticket, too early - e.g. a late entry ticket shown at 8pm. */
    NOT_YET_VALID,
    /** The entry window of this ticket type has closed. */
    EXPIRED
}
